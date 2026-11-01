package io.github.son30kins.workbench.application.calendar;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.application.config.WorkbenchProperties;
import io.github.son30kins.workbench.application.i18n.Translator;
import io.github.son30kins.workbench.domain.shared.NotFoundException;
import io.github.son30kins.workbench.domain.task.TaskRepository;
import io.github.son30kins.workbench.domain.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Link lịch iCal riêng của mỗi người dùng: URL chứa token bí mật (32 byte ngẫu nhiên); database chỉ lưu SHA-256 của
 * token nên lộ database cũng không lộ link. Tạo link mới = link cũ hết hiệu lực.
 */
@Service
@RequiredArgsConstructor
public class CalendarFeedService {

	private static final SecureRandom RANDOM = new SecureRandom();

	/** Mỗi task hiện thành sự kiện 30 phút kết thúc đúng hạn chót. */
	private static final int EVENT_MINUTES = 30;

	private final UserRepository users;

	private final TaskRepository tasks;

	private final Translator translator;

	private final WorkbenchProperties properties;

	private final Clock clock;

	@Transactional(readOnly = true)
	public boolean enabled(UUID userId) {
		return users.findById(userId).map(u -> u.getCalendarTokenHash() != null).orElse(false);
	}

	/** Tạo link mới (thu hồi link cũ). Token chỉ trả về đúng lần này. */
	@Transactional
	public String createLink(UUID userId) {
		var bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		var user = users.findById(userId).orElseThrow(() -> new NotFoundException("error.user.notFound"));
		user.changeCalendarToken(hash(token));
		return properties.appBaseUrl() + "/ical/" + token + ".ics";
	}

	@Transactional
	public void revoke(UUID userId) {
		users.findById(userId).ifPresent(u -> u.changeCalendarToken(null));
	}

	/** Nội dung .ics cho token; token sai / đã thu hồi → rỗng. */
	@Transactional(readOnly = true)
	public Optional<String> feed(String token) {
		if (token == null || token.length() > 100) {
			return Optional.empty();
		}
		return users.findByCalendarTokenHash(hash(token)).filter(u -> !u.isBanned()).map(user -> {
			var locale = user.getLanguage().locale();
			var calendar = new IcsCalendar("Workbench", clock.instant());
			for (var task : tasks.findOpenWithDueDate(user.getId())) {
				var priority = translator.get(locale, "priority." + task.getPriority().name());
				calendar.event(task.getId() + "@workbench", task.getDueAt(), EVENT_MINUTES,
						task.getPriority().icon() + " " + task.getTitle(),
						priority + (task.getDescription().isBlank() ? "" : "\n\n" + task.getDescription()),
						properties.appBaseUrl() + "/?task=" + task.getId());
			}
			return calendar.build();
		});
	}

	static String hash(String token) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
