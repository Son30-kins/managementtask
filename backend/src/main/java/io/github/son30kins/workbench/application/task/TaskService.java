package io.github.son30kins.workbench.application.task;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.application.reminder.ReminderService;
import io.github.son30kins.workbench.domain.shared.TimeZones;
import io.github.son30kins.workbench.domain.shared.ValidationException;
import io.github.son30kins.workbench.domain.shared.Violation;
import io.github.son30kins.workbench.domain.task.SubtaskRepository;
import io.github.son30kins.workbench.domain.task.Task;
import io.github.son30kins.workbench.domain.task.TaskCounts;
import io.github.son30kins.workbench.domain.task.TaskData;
import io.github.son30kins.workbench.domain.task.TaskSearch;
import io.github.son30kins.workbench.domain.task.TaskRepository;
import io.github.son30kins.workbench.domain.task.TaskStatus;
import io.github.son30kins.workbench.domain.task.TaskView;
import io.github.son30kins.workbench.domain.user.UserContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case về task. MỌI phương thức nhận chủ dữ liệu ({@link UserContext}) và chỉ đọc / sửa task của người đó —
 * task của người khác được coi như không tồn tại (Optional.empty / false).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TaskService {

	/** Cột "Hoàn thành" chỉ hiện 50 task xong gần nhất. */
	private static final int DONE_LIMIT = 50;

	/** Một màn hình lịch (tối đa 6 tuần) — đủ rộng, vẫn chặn trường hợp bất thường. */
	private static final int CALENDAR_LIMIT = 1000;

	private final TaskRepository tasks;

	private final SubtaskRepository subtasks;

	private final ReminderService reminders;

	private final Clock clock;

	/** Việc đang mở (theo ưu tiên, hạn) + tối đa 50 việc xong gần nhất. */
	@Transactional(readOnly = true)
	public List<Task> list(UserContext owner, TaskView view, String text) {
		var now = clock.instant();
		var q = text == null || text.isBlank() ? null : text.trim();
		var result = new ArrayList<>(tasks.search(view.openTasks(owner.id(), owner.zone(), now, q)));
		view.doneTasks(owner.id(), owner.zone(), now, q, DONE_LIMIT).ifPresent(s -> result.addAll(tasks.search(s)));
		return result;
	}

	@Transactional(readOnly = true)
	public Optional<Task> get(UserContext owner, UUID taskId) {
		return tasks.findOwned(taskId, owner.id());
	}

	public Task create(UserContext owner, TaskData data) {
		return create(owner, data, null, null);
	}

	/** Tạo task; {@code runId}/{@code templateName} khác null khi task sinh từ form mẫu. */
	public Task create(UserContext owner, TaskData data, UUID runId, String templateName) {
		var task = new Task(owner.id());
		task.apply(normalize(data, owner), clock.instant());
		if (runId != null) {
			task.linkTemplateRun(runId, templateName);
		}
		tasks.save(task);
		reminders.reschedule(task, owner);
		return task;
	}

	public Optional<Task> update(UserContext owner, UUID taskId, TaskData data) {
		return tasks.findOwned(taskId, owner.id()).map(task -> {
			if (task.apply(normalize(data, owner), clock.instant())) {
				reminders.reschedule(task, owner);
			}
			spawnNextOccurrence(owner, task);
			return task;
		});
	}

	public Optional<Task> changeStatus(UserContext owner, UUID taskId, TaskStatus status) {
		return tasks.findOwned(taskId, owner.id()).map(task -> {
			var previous = task.getStatus();
			if (previous == status) {
				return task;
			}
			task.changeStatus(status, clock.instant());
			if (status.isClosed()) {
				reminders.cancelPending(taskId);
			}
			else if (previous.isClosed()) {
				reminders.reschedule(task, owner);
			}
			spawnNextOccurrence(owner, task);
			return task;
		});
	}

	/** Task lặp lại vừa hoàn thành → tạo lần kế tiếp (cùng nguồn form mẫu nếu có). */
	private void spawnNextOccurrence(UserContext owner, Task done) {
		done.nextOccurrence(owner.zone(), clock.instant())
			.ifPresent(next -> create(owner, next, done.getTemplateRunId(), done.getTemplateName()));
	}

	/** Lặp lại cần có hạn chót làm mốc; lặp hằng tháng ghi lại ngày trong tháng của hạn. */
	private static TaskData normalize(TaskData data, UserContext owner) {
		if (data.recurrence() == null) {
			return data;
		}
		if (data.dueAt() == null) {
			throw new ValidationException(List.of(Violation.of("due", "error.task.recurrenceNeedsDue")));
		}
		return new TaskData(data.title(), data.description(), data.dueAt(), data.priority(), data.status(), data.tags(),
				data.subtasks(), data.recurrence().anchoredTo(data.dueAt(), owner.zone()));
	}

	/** Task có hạn trong các ngày [from, to] (theo lịch địa phương), cả việc đã xong — trang Lịch. */
	@Transactional(readOnly = true)
	public List<Task> calendar(UserContext owner, LocalDate from, LocalDate to) {
		var start = TimeZones.fromLocal(from, LocalTime.MIDNIGHT, owner.zone());
		var end = TimeZones.fromLocal(to.plusDays(1), LocalTime.MIDNIGHT, owner.zone());
		var statuses = EnumSet.copyOf(TaskStatus.ACTIVE);
		statuses.add(TaskStatus.DONE);
		return tasks.search(new TaskSearch(owner.id(), start, end, statuses, null, CALENDAR_LIMIT, TaskSearch.Order.DUE));
	}

	/** Đổi hạn chót (kéo thả trên Lịch) rồi tính lại lịch nhắc. */
	public Optional<Task> moveDue(UserContext owner, UUID taskId, Instant due) {
		return tasks.findOwned(taskId, owner.id()).map(task -> {
			task.moveDue(due);
			if (task.isOpen()) {
				reminders.reschedule(task, owner);
			}
			return task;
		});
	}

	public boolean delete(UserContext owner, UUID taskId) {
		return tasks.deleteOwned(taskId, owner.id());
	}

	/** Chỉ sửa được task con thuộc task của chính người dùng. */
	public boolean toggleSubtask(UserContext owner, UUID subtaskId, boolean done) {
		return subtasks.findOwned(subtaskId, owner.id()).map(subtask -> {
			subtask.markDone(done);
			return true;
		}).orElse(false);
	}

	@Transactional(readOnly = true)
	public TaskCounts counts(UserContext owner) {
		Instant now = clock.instant();
		var today = TimeZones.startOfLocalDay(now, owner.zone());
		return tasks.countForPanel(owner.id(), now, today, TimeZones.addLocalDays(today, 1, owner.zone()),
				TimeZones.addLocalDays(today, 7, owner.zone()));
	}

	/** Việc quá hạn + đến hạn hôm nay, chưa xong — bản tin buổi sáng và lệnh /today. */
	@Transactional(readOnly = true)
	public List<Task> digest(UserContext owner) {
		var tomorrow = TimeZones.addLocalDays(TimeZones.startOfLocalDay(clock.instant(), owner.zone()), 1, owner.zone());
		return tasks.findOpenDueBefore(owner.id(), tomorrow);
	}

	/** Hoãn nhắc một task đang mở. */
	public boolean snooze(UserContext owner, UUID taskId, Instant fireAt) {
		return tasks.findOwned(taskId, owner.id()).filter(Task::isOpen).map(task -> {
			reminders.snooze(task, fireAt);
			return true;
		}).orElse(false);
	}

	/** Tính lại lịch nhắc cho mọi task đang mở — sau khi người dùng đổi múi giờ / cài đặt nhắc. */
	public void rescheduleOpenTasks(UserContext owner) {
		tasks.findOpenWithDueDate(owner.id()).forEach(task -> reminders.reschedule(task, owner));
	}

}
