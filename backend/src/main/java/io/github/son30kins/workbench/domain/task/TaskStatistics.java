package io.github.son30kins.workbench.domain.task;

import java.time.LocalDate;

/** Số liệu đã gom nhóm cho trang Thống kê và báo cáo tuần (value object, chỉ đọc). */
public final class TaskStatistics {

	private TaskStatistics() {
	}

	/** Một tuần (bắt đầu thứ 2 theo giờ địa phương): tổng số và số trễ hạn. */
	public record WeekCount(LocalDate weekStart, long total, long late) {
	}

	/** Việc đang mở theo mức ưu tiên và số đang quá hạn. */
	public record PriorityCount(Priority priority, long open, long overdue) {
	}

	/** Việc hoàn thành trong một khoảng: tổng, số có hạn chót, số xong sau hạn. */
	public record CompletionCount(long total, long withDue, long late) {

		/** % xong đúng hạn trong số việc có hạn; null khi chưa có việc nào có hạn. */
		public Integer onTimeRate() {
			return withDue == 0 ? null : (int) Math.round(100.0 * (withDue - late) / withDue);
		}
	}

}
