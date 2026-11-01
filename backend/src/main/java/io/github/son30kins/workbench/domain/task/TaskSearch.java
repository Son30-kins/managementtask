package io.github.son30kins.workbench.domain.task;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Bộ lọc tìm task (value object). Domain mô tả CẦN lọc gì; tầng infrastructure quyết định truy vấn SQL ra sao.
 *
 * @param dueFrom hạn ≥ giá trị này (null = không giới hạn)
 * @param dueBefore hạn &lt; giá trị này (null = không giới hạn)
 * @param statuses chỉ lấy các trạng thái này
 * @param text tìm trong tiêu đề / mô tả (null = không tìm)
 * @param limit số kết quả tối đa (0 = không giới hạn)
 * @param sort thứ tự sắp xếp
 */
public record TaskSearch(UUID userId, Instant dueFrom, Instant dueBefore, Set<TaskStatus> statuses, String text,
		int limit, Order sort) {

	public enum Order {

		/** Ưu tiên cao trước, rồi hạn gần trước (không hạn xếp cuối), rồi mới tạo trước. */
		BOARD,
		/** Hoàn thành gần nhất trước. */
		RECENTLY_COMPLETED,
		/** Hạn sớm trước — trang Lịch. */
		DUE

	}

}
