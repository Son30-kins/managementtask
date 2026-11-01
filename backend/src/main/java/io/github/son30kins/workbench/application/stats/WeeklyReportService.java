package io.github.son30kins.workbench.application.stats;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.son30kins.workbench.application.i18n.Translator;
import io.github.son30kins.workbench.application.notification.MessageFormatter;
import io.github.son30kins.workbench.application.notification.NotificationService;
import io.github.son30kins.workbench.application.telegram.TelegramNotificationService;
import io.github.son30kins.workbench.domain.shared.TimeZones;
import io.github.son30kins.workbench.domain.task.TaskRepository;
import io.github.son30kins.workbench.domain.task.TaskStatistics;
import io.github.son30kins.workbench.domain.user.User;
import io.github.son30kins.workbench.domain.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Báo cáo tuần: sáng thứ 2 (đúng giờ bản tin buổi sáng của từng người) gửi tóm tắt tuần trước qua chuông thông báo
 * và Telegram. Mỗi người tối đa một lần mỗi tuần; tuần không có hoạt động gì thì bỏ qua.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportService {

	private final UserRepository users;

	private final TaskRepository tasks;

	private final NotificationService notifications;

	private final TelegramNotificationService telegram;

	private final Translator translator;

	private final TransactionTemplate tx;

	private final Clock clock;

	/** Nội dung báo cáo: tiêu đề + tóm tắt (thông báo trong app) + HTML (Telegram). */
	public record Report(String title, String summary, String html) {
	}

	/** @return số báo cáo đã gửi */
	public int sendDue() {
		var now = clock.instant();
		int sent = 0;
		for (var user : tx.execute(status -> users.findActive())) {
			var settings = user.getEffectiveSettings();
			var zone = user.getZone();
			var today = TimeZones.localDate(now, zone);
			if (!settings.weeklyReportEnabled() || today.getDayOfWeek() != DayOfWeek.MONDAY
					|| now.atZone(zone).toLocalTime().isBefore(settings.digest().localTime())
					|| today.equals(user.getLastWeeklyReportOn())) {
				continue;
			}
			// Đánh dấu trước khi gửi → không gửi trùng nếu chạy nhiều instance
			if (!Boolean.TRUE.equals(tx.execute(status -> users.claimWeeklyReport(user.getId(), today)))) {
				continue;
			}
			var report = tx.execute(status -> build(user, now));
			if (report == null) {
				continue;
			}
			notifications.add(user.getId(), null, report.title(), report.summary());
			try {
				telegram.sendTo(user.getId(), report.html(), null, user.getLanguage().locale());
			}
			catch (RuntimeException ex) {
				log.error("Lỗi gửi báo cáo tuần cho {}: {}", user.getId(), ex.getMessage());
			}
			sent++;
		}
		return sent;
	}

	/** Báo cáo cho tuần trước (thứ 2 → chủ nhật, giờ địa phương); null nếu tuần đó không có gì để báo. */
	Report build(User user, Instant now) {
		var zone = user.getZone();
		Locale locale = user.getLanguage().locale();
		var weekStart = StatsService.startOfWeek(now, zone).minus(Duration.ofDays(7));
		var weekEnd = weekStart.plus(Duration.ofDays(7));

		TaskStatistics.CompletionCount done = tasks.completedBetween(user.getId(), weekStart, weekEnd);
		long previous = tasks.completedBetween(user.getId(), weekStart.minus(Duration.ofDays(7)), weekStart).total();
		long created = tasks.countCreatedBetween(user.getId(), weekStart, weekEnd);
		long overdue = tasks.openByPriority(user.getId(), now).stream().mapToLong(TaskStatistics.PriorityCount::overdue).sum();
		if (done.total() == 0 && created == 0 && overdue == 0) {
			return null;
		}
		long onTime = done.total() - done.late();
		var range = TimeZones.format(weekStart, zone, "dd/MM", locale) + " – "
				+ TimeZones.format(weekEnd.minusSeconds(1), zone, "dd/MM", locale);

		var title = translator.get(locale, "weekly.title", range);
		var lines = new ArrayList<String>();
		lines.add("📊 <b>" + MessageFormatter.escapeHtml(title) + "</b>");
		lines.add("✅ " + translator.get(locale, "weekly.completed", done.total(), onTime, done.late()));
		lines.add("🆕 " + translator.get(locale, "weekly.created", created));
		lines.add("🔴 " + translator.get(locale, "weekly.overdue", overdue));
		long change = done.total() - previous;
		lines.add(change > 0 ? "📈 " + translator.get(locale, "weekly.more", change)
				: change < 0 ? "📉 " + translator.get(locale, "weekly.fewer", -change)
						: "➖ " + translator.get(locale, "weekly.same"));
		return new Report(title, translator.get(locale, "weekly.summary", done.total(), onTime, overdue),
				String.join("\n", lines));
	}

}
