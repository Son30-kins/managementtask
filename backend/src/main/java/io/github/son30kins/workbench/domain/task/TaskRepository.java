package io.github.son30kins.workbench.domain.task;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Port lưu trữ task. Các phương thức cho người dùng đều nhận {@code userId} — không có cách nào đọc / sửa
 * task của người khác. {@link #findById} chỉ dành cho job hệ thống.
 */
public interface TaskRepository {

	Task save(Task task);

	/** Chỉ dùng cho job hệ thống (gửi nhắc việc) — không trả dữ liệu ra ngoài. */
	Optional<Task> findById(UUID id);

	Optional<Task> findOwned(UUID id, UUID userId);

	boolean deleteOwned(UUID id, UUID userId);

	/** Tìm task theo bộ lọc (xem {@link TaskSearch}). */
	List<Task> search(TaskSearch search);

	/** Việc đang mở có hạn trước {@code before} — bản tin buổi sáng, lệnh /today. */
	List<Task> findOpenDueBefore(UUID userId, Instant before);

	/** Việc đang mở có hạn — để tính lại lịch nhắc khi đổi cài đặt. */
	List<Task> findOpenWithDueDate(UUID userId);

	TaskCounts countForPanel(UUID userId, Instant now, Instant today, Instant tomorrow, Instant weekEnd);

	/** Số task của từng người dùng — trang quản trị. */
	Map<UUID, Long> countByUser();

	// ===== Thống kê (tuần bắt đầu thứ 2 theo múi giờ zone) =====

	List<TaskStatistics.WeekCount> completedByWeek(UUID userId, ZoneId zone, Instant from);

	List<TaskStatistics.WeekCount> createdByWeek(UUID userId, ZoneId zone, Instant from);

	List<TaskStatistics.PriorityCount> openByPriority(UUID userId, Instant now);

	TaskStatistics.CompletionCount completedBetween(UUID userId, Instant from, Instant to);

	long countCreatedBetween(UUID userId, Instant from, Instant to);

}
