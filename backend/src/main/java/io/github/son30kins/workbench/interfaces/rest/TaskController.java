package io.github.son30kins.workbench.interfaces.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.application.task.TaskService;
import io.github.son30kins.workbench.domain.shared.DomainException;
import io.github.son30kins.workbench.domain.shared.NotFoundException;
import io.github.son30kins.workbench.domain.shared.TimeZones;
import io.github.son30kins.workbench.domain.task.TaskCounts;
import io.github.son30kins.workbench.domain.task.TaskView;
import io.github.son30kins.workbench.infrastructure.security.CurrentUser;
import io.github.son30kins.workbench.interfaces.rest.dto.request.SubtaskDoneRequest;
import io.github.son30kins.workbench.interfaces.rest.dto.request.TaskRequest;
import io.github.son30kins.workbench.interfaces.rest.dto.request.TaskStatusRequest;
import io.github.son30kins.workbench.interfaces.rest.dto.response.TaskResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class TaskController {

	private final TaskService tasks;

	/** Việc đang mở (theo ưu tiên, hạn) + 50 việc xong gần nhất. {@code view}: all | today | overdue | week. */
	@GetMapping("/tasks")
	List<TaskResponse> list(CurrentUser user, @RequestParam(required = false) String view,
			@RequestParam(required = false) String q) {
		return tasks.list(user.context(), TaskView.parse(view), q).stream().map(TaskResponse::from).toList();
	}

	/** Trang Lịch: task có hạn trong [from, to] (ngày theo múi giờ người dùng, tối đa 62 ngày). */
	@GetMapping("/tasks/calendar")
	List<TaskResponse> calendar(CurrentUser user, @RequestParam LocalDate from, @RequestParam LocalDate to) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 62) {
			throw new DomainException("error.validation");
		}
		return tasks.calendar(user.context(), from, to).stream().map(TaskResponse::from).toList();
	}

	/** Kéo task sang ngày khác: {"due": "2026-11-05T17:00"} theo giờ của người dùng. */
	@PatchMapping("/tasks/{id}/due")
	TaskResponse moveDue(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody DueRequest request) {
		return tasks.moveDue(user.context(), id, TimeZones.fromLocal(request.due(), user.zone()))
			.map(TaskResponse::from)
			.orElseThrow(TaskController::notFound);
	}

	/** Body PATCH /api/tasks/{id}/due. */
	record DueRequest(@NotNull LocalDateTime due) {
	}

	@GetMapping("/tasks/counts")
	TaskCounts counts(CurrentUser user) {
		return tasks.counts(user.context());
	}

	@GetMapping("/tasks/{id}")
	TaskResponse get(CurrentUser user, @PathVariable UUID id) {
		return tasks.get(user.context(), id).map(TaskResponse::from).orElseThrow(TaskController::notFound);
	}

	@PostMapping("/tasks")
	@ResponseStatus(HttpStatus.CREATED)
	TaskResponse create(CurrentUser user, @Valid @RequestBody TaskRequest request) {
		return TaskResponse.from(tasks.create(user.context(), request.toData(user.zone())));
	}

	@PutMapping("/tasks/{id}")
	TaskResponse update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
		return tasks.update(user.context(), id, request.toData(user.zone()))
			.map(TaskResponse::from)
			.orElseThrow(TaskController::notFound);
	}

	@PatchMapping("/tasks/{id}/status")
	TaskResponse changeStatus(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody TaskStatusRequest request) {
		return tasks.changeStatus(user.context(), id, request.status())
			.map(TaskResponse::from)
			.orElseThrow(TaskController::notFound);
	}

	@DeleteMapping("/tasks/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(CurrentUser user, @PathVariable UUID id) {
		if (!tasks.delete(user.context(), id)) {
			throw notFound();
		}
	}

	@PatchMapping("/subtasks/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void toggleSubtask(CurrentUser user, @PathVariable UUID id, @RequestBody SubtaskDoneRequest request) {
		if (!tasks.toggleSubtask(user.context(), id, request.done())) {
			throw new NotFoundException("error.subtask.notFound");
		}
	}

	private static NotFoundException notFound() {
		return new NotFoundException("error.task.notFound");
	}

}
