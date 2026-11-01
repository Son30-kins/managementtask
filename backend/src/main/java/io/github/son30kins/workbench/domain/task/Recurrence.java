package io.github.son30kins.workbench.domain.task;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.github.son30kins.workbench.domain.shared.TimeZones;

/**
 * Quy tắc lặp lại của một task (value object, lưu jsonb). Lần kế tiếp tính từ hạn chót hiện tại, giữ nguyên giờ trên
 * đồng hồ theo múi giờ người dùng.
 *
 * @param frequency lặp theo ngày / tuần / tháng
 * @param interval mỗi bao nhiêu ngày / tuần / tháng (≥ 1)
 * @param weekdays chỉ với WEEKLY: các thứ trong tuần (rỗng = cùng thứ với hạn hiện tại)
 * @param monthDay chỉ với MONTHLY: ngày trong tháng (31 → ngày cuối ở tháng ngắn, không bị trôi về 28)
 */
public record Recurrence(@NotNull Frequency frequency, @Min(1) @Max(365) int interval, Set<DayOfWeek> weekdays,
		@Min(1) @Max(31) Integer monthDay) {

	public enum Frequency {

		DAILY, WEEKLY, MONTHLY

	}

	public Recurrence {
		weekdays = frequency == Frequency.WEEKLY && weekdays != null && !weekdays.isEmpty() ? Set.copyOf(weekdays)
				: Set.of();
		monthDay = frequency == Frequency.MONTHLY ? monthDay : null;
	}

	/** Lặp hằng tháng chưa ghi ngày → lấy ngày của hạn hiện tại. */
	public Recurrence anchoredTo(Instant due, ZoneId zone) {
		if (frequency != Frequency.MONTHLY || monthDay != null || due == null) {
			return this;
		}
		return new Recurrence(frequency, interval, weekdays, due.atZone(zone).getDayOfMonth());
	}

	/** Hạn kế tiếp sau {@code due}; bỏ qua các lần đã nằm trong quá khứ (so với {@code notBefore}). */
	public Instant next(Instant due, ZoneId zone, Instant notBefore) {
		var local = due.atZone(zone).toLocalDateTime();
		for (int i = 0; i < 10_000; i++) {
			local = step(local, zone, due);
			var candidate = TimeZones.fromLocal(local, zone);
			if (candidate.isAfter(notBefore)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Recurrence did not reach " + notBefore);
	}

	/**
	 * Lần đầu tiên khớp quy tắc, lúc {@code time} trên đồng hồ, sau {@code after} — dùng cho lịch tự chạy (chưa có mốc
	 * hạn chót). Hằng tuần cần chọn thứ (rỗng = mọi ngày), hằng tháng cần ngày trong tháng.
	 */
	public Instant firstOccurrence(LocalTime time, ZoneId zone, Instant after) {
		var start = after.atZone(zone).toLocalDate();
		for (int i = 0; i < 800; i++) {
			var day = start.plusDays(i);
			var at = TimeZones.fromLocal(day, time, zone);
			if (matches(day) && at.isAfter(after)) {
				return at;
			}
		}
		throw new IllegalStateException("No occurrence after " + after);
	}

	private boolean matches(LocalDate day) {
		return switch (frequency) {
			case DAILY -> true;
			case WEEKLY -> weekdays.isEmpty() || weekdays.contains(day.getDayOfWeek());
			case MONTHLY -> day.getDayOfMonth() == Math.min(monthDay == null ? 1 : monthDay, day.lengthOfMonth());
		};
	}

	private LocalDateTime step(LocalDateTime from, ZoneId zone, Instant due) {
		return switch (frequency) {
			case DAILY -> from.plusDays(interval);
			case MONTHLY -> {
				var day = monthDay != null ? monthDay : due.atZone(zone).getDayOfMonth();
				var month = from.plusMonths(interval);
				yield month.withDayOfMonth(Math.min(day, month.toLocalDate().lengthOfMonth()));
			}
			case WEEKLY -> {
				if (weekdays.isEmpty()) {
					yield from.plusWeeks(interval);
				}
				var days = EnumSet.copyOf(weekdays);
				// Thứ tiếp theo trong cùng tuần (tuần bắt đầu thứ 2)…
				for (var d = from.getDayOfWeek().getValue() + 1; d <= 7; d++) {
					if (days.contains(DayOfWeek.of(d))) {
						yield from.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(d)));
					}
				}
				// …hết tuần thì sang thứ đầu tiên của tuần sau `interval` tuần
				var weekStart = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(interval);
				yield weekStart.with(TemporalAdjusters.nextOrSame(days.iterator().next()));
			}
		};
	}

}
