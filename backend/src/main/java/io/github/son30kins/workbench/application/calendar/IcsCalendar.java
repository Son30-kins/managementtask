package io.github.son30kins.workbench.application.calendar;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Dựng file iCalendar (RFC 5545) cho Google Calendar / Apple Calendar / Outlook: dòng kết thúc CRLF, dòng dài hơn
 * 75 byte gập lại, ký tự đặc biệt trong nội dung được escape.
 */
public final class IcsCalendar {

	private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

	private final StringBuilder out = new StringBuilder();

	/** Thời điểm tạo file — DTSTAMP của mọi sự kiện. */
	private final String stamp;

	public IcsCalendar(String name, Instant now) {
		line("BEGIN:VCALENDAR");
		line("VERSION:2.0");
		line("PRODID:-//Workbench//Tasks//EN");
		line("CALSCALE:GREGORIAN");
		line("METHOD:PUBLISH");
		line("X-WR-CALNAME:" + escape(name));
		// Gợi ý ứng dụng lịch tải lại mỗi giờ (Google tự quyết, thường vài giờ)
		line("REFRESH-INTERVAL;VALUE=DURATION:PT1H");
		line("X-PUBLISHED-TTL:PT1H");
		this.stamp = UTC.format(now);
	}

	/** Một sự kiện dài {@code minutes} phút kết thúc đúng hạn chót. */
	public IcsCalendar event(String uid, Instant due, int minutes, String summary, String description, String url) {
		line("BEGIN:VEVENT");
		line("UID:" + uid);
		line("DTSTAMP:" + stamp);
		line("DTSTART:" + UTC.format(due.minusSeconds(minutes * 60L)));
		line("DTEND:" + UTC.format(due));
		line("SUMMARY:" + escape(summary));
		if (description != null && !description.isBlank()) {
			line("DESCRIPTION:" + escape(description));
		}
		if (url != null) {
			line("URL:" + url);
		}
		line("END:VEVENT");
		return this;
	}

	public String build() {
		line("END:VCALENDAR");
		return out.toString();
	}

	static String escape(String text) {
		return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n");
	}

	/** Gập dòng: tối đa 75 byte UTF-8 mỗi dòng, dòng nối bắt đầu bằng một dấu cách; không cắt giữa ký tự. */
	private void line(String content) {
		int bytes = 0;
		int limit = 75;
		for (int i = 0; i < content.length();) {
			int cp = content.codePointAt(i);
			int size = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
			if (bytes + size > limit) {
				out.append("\r\n ");
				bytes = 1;
				limit = 75;
			}
			out.appendCodePoint(cp);
			bytes += size;
			i += Character.charCount(cp);
		}
		out.append("\r\n");
	}

}
