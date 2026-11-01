package io.github.son30kins.workbench.interfaces.rest.dto.request;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.github.son30kins.workbench.domain.shared.TimeZones;
import io.github.son30kins.workbench.domain.task.Priority;
import io.github.son30kins.workbench.domain.task.Recurrence;
import io.github.son30kins.workbench.domain.task.TaskData;
import io.github.son30kins.workbench.domain.task.TaskStatus;

/**
 * Body tạo / sửa task. {@code due} là giờ trên đồng hồ của người dùng ("2026-10-31T17:00") —
 * server đổi sang thời điểm theo múi giờ trong cài đặt của họ.
 */
public record TaskRequest(@NotBlank(message = "{validation.task.title}") @Size(max = 200) String title,
		@Size(max = 5000) String description, LocalDateTime due, @NotNull Priority priority, @NotNull TaskStatus status,
		@Size(max = 10) List<@NotNull @Size(max = 40) String> tags,
		@Size(max = 50) List<@Valid @NotNull SubtaskItem> subtasks, @Valid Recurrence recurrence) {

	public record SubtaskItem(@Size(max = 200) String title, boolean done) {
	}

	/** DTO → value object của domain. */
	public TaskData toData(ZoneId zone) {
		var dueAt = due == null ? null : TimeZones.fromLocal(due, zone);
		var items = subtasks == null ? List.<TaskData.SubtaskData>of()
				: subtasks.stream().map(s -> new TaskData.SubtaskData(s.title(), s.done())).toList();
		return new TaskData(title.trim(), description, dueAt, priority, status, tags, items, recurrence);
	}

}
