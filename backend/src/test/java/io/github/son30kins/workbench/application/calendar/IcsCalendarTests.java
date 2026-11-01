package io.github.son30kins.workbench.application.calendar;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** File iCalendar hợp lệ: CRLF, escape, gập dòng ≤ 75 byte không cắt đôi ký tự UTF-8. */
class IcsCalendarTests {

	static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

	@Test
	void suKien_dungDinhDang() {
		var ics = new IcsCalendar("Workbench", NOW)
			.event("abc@workbench", Instant.parse("2026-11-02T10:00:00Z"), 30, "Họp; tổng kết, quý 4", "Dòng 1\nDòng 2",
					"https://app.example/?task=abc")
			.build();
		assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
		assertThat(ics).contains("DTSTART:20261102T093000Z\r\n", "DTEND:20261102T100000Z\r\n",
				"SUMMARY:Họp\\; tổng kết\\, quý 4\r\n", "DESCRIPTION:Dòng 1\\nDòng 2\r\n", "UID:abc@workbench\r\n");
		assertThat(ics.replace("\r\n", "")).doesNotContain("\n");
	}

	@Test
	void gapDongDai_khongQua75Byte_khongCatKyTu() {
		var title = "Chuẩn bị tài liệu hướng dẫn cài đặt chi tiết cho người dùng mới ở công ty ".repeat(4);
		var ics = new IcsCalendar("Workbench", NOW).event("x", NOW, 30, title, null, null).build();
		for (var line : ics.split("\r\n")) {
			assertThat(line.getBytes(StandardCharsets.UTF_8).length).as(line).isLessThanOrEqualTo(75);
		}
		// Ghép lại (bỏ CRLF + dấu cách đầu dòng nối) ra đúng tiêu đề ban đầu
		assertThat(ics.replace("\r\n ", "")).contains("SUMMARY:" + title.replace(",", "\\,"));
	}

}
