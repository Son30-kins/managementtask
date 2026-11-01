package io.github.son30kins.workbench.application.template;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.son30kins.workbench.application.i18n.Translator;
import io.github.son30kins.workbench.application.notification.MessageFormatter;
import io.github.son30kins.workbench.application.notification.NotificationService;
import io.github.son30kins.workbench.application.telegram.TelegramNotificationService;
import io.github.son30kins.workbench.domain.shared.TimeZones;
import io.github.son30kins.workbench.domain.shared.ValidationException;
import io.github.son30kins.workbench.domain.shared.Violation;
import io.github.son30kins.workbench.domain.task.Recurrence;
import io.github.son30kins.workbench.domain.template.InvalidFormValuesException;
import io.github.son30kins.workbench.domain.template.TemplateEngine;
import io.github.son30kins.workbench.domain.template.TemplateRepository;
import io.github.son30kins.workbench.domain.template.TemplateSchedule;
import io.github.son30kins.workbench.domain.template.TemplateScheduleRepository;
import io.github.son30kins.workbench.domain.user.UserContext;
import io.github.son30kins.workbench.domain.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lịch tự chạy của form mẫu: người dùng đặt lịch + giá trị form; job định kỳ ({@link #runDue}) đến giờ thì tạo task,
 * báo trong app và qua Telegram. Giá trị không còn hợp lệ (form mẫu đã sửa) → tắt lịch và báo người dùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateScheduleService {

	private static final int BATCH = 20;

	private final TemplateScheduleRepository schedules;

	private final TemplateRepository templates;

	private final TemplateService templateService;

	private final UserRepository users;

	private final NotificationService notifications;

	private final TelegramNotificationService telegram;

	private final Translator translator;

	private final TransactionTemplate tx;

	private final Clock clock;

	/** Dữ liệu người dùng gửi khi đặt lịch. */
	public record ScheduleInput(boolean enabled, Recurrence recurrence, LocalTime time, Map<String, Object> values) {
	}

	@Transactional(readOnly = true)
	public Optional<TemplateSchedule> get(UUID userId, UUID templateId) {
		return schedules.findOwned(templateId, userId);
	}

	@Transactional(readOnly = true)
	public List<TemplateSchedule> list(UUID userId) {
		return schedules.findByUser(userId);
	}

	/**
	 * Đặt / đổi lịch. Kiểm tra giá trị form ngay (với ngày chạy = hôm nay) để báo lỗi lúc lưu, không đợi tới giờ chạy.
	 * @return rỗng nếu form mẫu không thuộc người dùng
	 */
	@Transactional
	public Optional<TemplateSchedule> save(UserContext owner, UUID templateId, ScheduleInput input) {
		return templates.findOwned(templateId, owner.id()).map(template -> {
			var recurrence = input.recurrence();
			if (recurrence.frequency() == Recurrence.Frequency.WEEKLY && recurrence.weekdays().isEmpty()) {
				throw new ValidationException(List.of(Violation.of("recurrence.weekdays", "error.schedule.weekdays")));
			}
			if (recurrence.frequency() == Recurrence.Frequency.MONTHLY && recurrence.monthDay() == null) {
				throw new ValidationException(List.of(Violation.of("recurrence.monthDay", "error.schedule.monthDay")));
			}
			var now = clock.instant();
			var preview = TemplateSchedule.resolve(template.getFields(), input.values(),
					TimeZones.localDate(now, owner.zone()));
			var validated = TemplateEngine.validateValues(template.getFields(), preview);
			if (validated.hasErrors()) {
				throw new InvalidFormValuesException(validated.errors());
			}
			var schedule = schedules.findOwned(templateId, owner.id())
				.orElseGet(() -> new TemplateSchedule(templateId, owner.id()));
			schedule.configure(input.enabled(), recurrence, input.time(), input.values(), owner.zone(), now);
			return schedules.save(schedule);
		});
	}

	@Transactional
	public boolean delete(UUID userId, UUID templateId) {
		return schedules.deleteOwned(templateId, userId);
	}

	private record Claimed(UUID templateId, UUID userId, Instant scheduledAt) {
	}

	private record Done(UUID userId, String html, Locale locale) {
	}

	/** Chạy các lịch đã đến giờ. @return số lịch đã lấy ra xử lý */
	public int runDue() {
		var now = clock.instant();
		// 1) Khoá + hẹn lần sau trong cùng transaction → mỗi lần chạy chỉ một instance thực hiện
		List<Claimed> claimed = tx.execute(status -> schedules.lockDue(now, BATCH).stream().flatMap(schedule -> users
			.findById(schedule.getUserId())
			.map(user -> new Claimed(schedule.getTemplateId(), user.getId(), schedule.claim(user.getZone(), now)))
			.stream()).toList());
		for (var c : claimed) {
			try {
				var done = tx.execute(status -> run(c));
				// 2) Gửi Telegram ngoài transaction
				if (done != null) {
					telegram.sendTo(done.userId(), done.html(), null, done.locale());
				}
			}
			catch (RuntimeException ex) {
				log.error("Lỗi chạy lịch form mẫu {}: {}", c.templateId(), ex.getMessage());
			}
		}
		return claimed.size();
	}

	private Done run(Claimed c) {
		var user = users.findById(c.userId()).orElse(null);
		var template = templates.findOwned(c.templateId(), c.userId()).orElse(null);
		var schedule = schedules.findOwned(c.templateId(), c.userId()).orElse(null);
		if (user == null || user.isBanned() || template == null || schedule == null) {
			return null;
		}
		UserContext owner = user.toContext();
		var locale = owner.locale();
		var values = schedule.valuesFor(template.getFields(), TimeZones.localDate(c.scheduledAt(), owner.zone()));
		// Kiểm tra trước khi chạy: lỗi xảy ra bên trong TemplateService sẽ đánh dấu rollback cả transaction
		var validated = TemplateEngine.validateValues(template.getFields(), values);
		if (validated.hasErrors()) {
			schedule.fail(String.join(", ", validated.errors().keySet()));
			var title = translator.get(locale, "schedule.failed", template.getName());
			notifications.add(user.getId(), null, title, translator.get(locale, "schedule.failedBody"));
			return new Done(user.getId(), "⚠️ <b>" + MessageFormatter.escapeHtml(title) + "</b>", locale);
		}
		int count = templateService.run(owner, template.getId(), values).orElse(0);
		var title = translator.get(locale, "schedule.ran", count, template.getName());
		notifications.add(user.getId(), null, title, "");
		return new Done(user.getId(), "🗂 <b>" + MessageFormatter.escapeHtml(title) + "</b>", locale);
	}

}
