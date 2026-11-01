package io.github.son30kins.workbench.domain.user;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.github.son30kins.workbench.domain.task.Priority;

/**
 * Cài đặt nhắc việc của từng người dùng — lưu trong cột jsonb {@code users.settings}.
 *
 * @param reminderOffsets nhắc trước hạn bao nhiêu phút, theo từng mức ưu tiên
 * @param overdueRepeatHours task quá hạn được nhắc lại sau mỗi bấy nhiêu giờ
 */
public record UserSettings(
		@NotNull Map<Priority, @NotNull @Size(max = 10) List<@NotNull @Min(1) @Max(43_200) Integer>> reminderOffsets,
		@NotNull @Valid QuietHours quietHours,
		@NotNull @Valid Digest digest,
		@Min(1) @Max(168) int overdueRepeatHours, Boolean weeklyReport) {

	public static final String HHMM = "^([01]\\d|2[0-3]):[0-5]\\d$";

	public static final UserSettings DEFAULTS = new UserSettings(
			new EnumMap<>(Map.of(Priority.URGENT, List.of(1440, 180, 60, 15), Priority.HIGH, List.of(1440, 120),
					Priority.MEDIUM, List.of(1440), Priority.LOW, List.of())),
			new QuietHours(true, "22:00", "07:00"), new Digest(true, "08:00"), 24, true);

	/** Cài đặt lưu trước khi có báo cáo tuần — coi như bật. */
	public UserSettings(Map<Priority, List<Integer>> reminderOffsets, QuietHours quietHours, Digest digest,
			int overdueRepeatHours) {
		this(reminderOffsets, quietHours, digest, overdueRepeatHours, null);
	}

	/** Báo cáo tuần sáng thứ 2 (null = bật — cài đặt lưu trước khi có tính năng này). */
	public boolean weeklyReportEnabled() {
		return weeklyReport == null || weeklyReport;
	}

	/** Giờ yên lặng: không nhắc (trừ task khẩn cấp). Hỗ trợ khung qua nửa đêm, ví dụ 22:00 → 07:00. */
	public record QuietHours(boolean enabled, @NotNull @Pattern(regexp = HHMM, message = "{validation.time}") String start,
			@NotNull @Pattern(regexp = HHMM, message = "{validation.time}") String end) {

		public LocalTime startTime() {
			return LocalTime.parse(start);
		}

		public LocalTime endTime() {
			return LocalTime.parse(end);
		}
	}

	/** Bản tin buổi sáng: danh sách việc hôm nay + quá hạn, gửi lúc {@code time}. */
	public record Digest(boolean enabled, @NotNull @Pattern(regexp = HHMM, message = "{validation.time}") String time) {

		public LocalTime localTime() {
			return LocalTime.parse(time);
		}
	}

	public List<Integer> offsetsFor(Priority priority) {
		return reminderOffsets.getOrDefault(priority, List.of());
	}

	@JsonIgnore
	@AssertTrue(message = "{validation.settings.complete}")
	public boolean isComplete() {
		return reminderOffsets != null && reminderOffsets.keySet().containsAll(List.of(Priority.values()));
	}

	/** Settings đọc từ DB: chưa có hoặc thiếu phần nào thì dùng mặc định. */
	public static UserSettings orDefaults(UserSettings stored) {
		if (stored == null || !stored.isComplete() || stored.quietHours() == null || stored.digest() == null) {
			return DEFAULTS;
		}
		return stored;
	}

}
