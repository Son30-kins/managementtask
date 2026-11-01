package io.github.son30kins.workbench.domain.task;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import io.github.son30kins.workbench.domain.shared.TimeZones;
import io.github.son30kins.workbench.domain.task.Recurrence.Frequency;
import org.junit.jupiter.api.Test;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

/** Tính hạn kế tiếp của task lặp lại — giữ giờ trên đồng hồ, không trôi ngày, bỏ qua lần đã lỡ. */
class RecurrenceTests {

	static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

	static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

	static Instant at(String local, ZoneId zone) {
		return TimeZones.fromLocal(LocalDateTime.parse(local), zone);
	}

	static LocalDateTime next(Recurrence r, String due, String now, ZoneId zone) {
		return r.next(at(due, zone), zone, at(now, zone)).atZone(zone).toLocalDateTime();
	}

	static Recurrence weekly(int interval, DayOfWeek... days) {
		return new Recurrence(Frequency.WEEKLY, interval, Set.of(days), null);
	}

	@Test
	void hangNgay() {
		var daily = new Recurrence(Frequency.DAILY, 1, null, null);
		assertThat(next(daily, "2026-11-02T08:00", "2026-11-02T09:00", VN)).isEqualTo("2026-11-03T08:00");
	}

	@Test
	void ngayLamViec_boQuaCuoiTuan() {
		var weekdays = weekly(1, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);
		// Thứ 6 06/11 → thứ 2 09/11
		assertThat(next(weekdays, "2026-11-06T17:00", "2026-11-06T18:00", VN)).isEqualTo("2026-11-09T17:00");
		assertThat(next(weekdays, "2026-11-03T17:00", "2026-11-03T18:00", VN)).isEqualTo("2026-11-04T17:00");
	}

	@Test
	void hangTuan_cungThuVoiHan() {
		var weekly = new Recurrence(Frequency.WEEKLY, 1, null, null);
		assertThat(next(weekly, "2026-11-02T08:00", "2026-11-02T09:00", VN)).isEqualTo("2026-11-09T08:00");
	}

	@Test
	void moi2Tuan_vaoThu2VaThu4() {
		var r = weekly(2, MONDAY, WEDNESDAY);
		assertThat(next(r, "2026-11-02T08:00", "2026-11-02T09:00", VN)).isEqualTo("2026-11-04T08:00");
		// Hết tuần → thứ 2 của tuần sau 2 tuần
		assertThat(next(r, "2026-11-04T08:00", "2026-11-04T09:00", VN)).isEqualTo("2026-11-16T08:00");
	}

	@Test
	void hangThangNgay31_khongBiTroiSau2Thang2() {
		var due = at("2027-01-31T09:00", VN);
		var r = new Recurrence(Frequency.MONTHLY, 1, null, null).anchoredTo(due, VN);
		var feb = r.next(due, VN, due);
		assertThat(feb.atZone(VN).toLocalDateTime()).isEqualTo("2027-02-28T09:00");
		assertThat(r.next(feb, VN, feb).atZone(VN).toLocalDateTime()).isEqualTo("2027-03-31T09:00");
	}

	@Test
	void hoanThanhTre_boQuaCacLanDaQua() {
		var daily = new Recurrence(Frequency.DAILY, 1, null, null);
		// Hạn 02/11, hoàn thành 05/11 lúc 10h → lần kế tiếp 06/11 (không tạo 03, 04, 05)
		assertThat(next(daily, "2026-11-02T08:00", "2026-11-05T10:00", VN)).isEqualTo("2026-11-06T08:00");
	}

	@Test
	void quaDoiGioMuaDong_vanGiu8GioSang() {
		var weekly = new Recurrence(Frequency.WEEKLY, 1, null, null);
		// Berlin đổi giờ ngày 25/10/2026
		assertThat(next(weekly, "2026-10-19T08:00", "2026-10-19T09:00", BERLIN)).isEqualTo("2026-10-26T08:00");
	}

	@Test
	void lanChayDauTien_choLichTuChay() {
		var mondays = weekly(1, MONDAY);
		// Chủ nhật 01/11 → thứ 2 02/11 lúc 08:00
		assertThat(mondays.firstOccurrence(LocalTime.of(8, 0), VN, at("2026-11-01T10:00", VN)).atZone(VN).toLocalDateTime())
			.isEqualTo("2026-11-02T08:00");
		// Hằng ngày, đã qua giờ hôm nay → ngày mai
		var daily = new Recurrence(Frequency.DAILY, 1, null, null);
		assertThat(daily.firstOccurrence(LocalTime.of(8, 0), VN, at("2026-11-01T10:00", VN)).atZone(VN).toLocalDateTime())
			.isEqualTo("2026-11-02T08:00");
		// Ngày 15 hằng tháng
		var monthly = new Recurrence(Frequency.MONTHLY, 1, null, 15);
		assertThat(monthly.firstOccurrence(LocalTime.of(9, 0), VN, at("2026-11-20T10:00", VN)).atZone(VN).toLocalDateTime())
			.isEqualTo("2026-12-15T09:00");
	}

}
