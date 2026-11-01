package io.github.son30kins.workbench.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.son30kins.workbench.domain.task.Priority;
import io.github.son30kins.workbench.domain.task.Recurrence;
import io.github.son30kins.workbench.domain.task.Task;
import io.github.son30kins.workbench.domain.task.TaskStatus;

public record TaskResponse(UUID id, String title, String description, Instant dueAt, Priority priority,
		TaskStatus status, List<String> tags, UUID templateRunId, String templateName, Instant completedAt,
		Instant createdAt, Instant updatedAt, List<SubtaskResponse> subtasks, Recurrence recurrence) {

	public record SubtaskResponse(UUID id, String title, boolean done, int position) {
	}

	/** Entity → DTO. Không trả entity ra ngoài để tránh lộ trường nội bộ và lỗi lazy loading. */
	public static TaskResponse from(Task t) {
		return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getDueAt(), t.getPriority(),
				t.getStatus(), List.copyOf(t.getTags()), t.getTemplateRunId(), t.getTemplateName(), t.getCompletedAt(),
				t.getCreatedAt(), t.getUpdatedAt(),
				t.getSubtasks()
					.stream()
					.map(s -> new SubtaskResponse(s.getId(), s.getTitle(), s.isDone(), s.getPosition()))
					.toList(),
				t.getRecurrence());
	}

}
