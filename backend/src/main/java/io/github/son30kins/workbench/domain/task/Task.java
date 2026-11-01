package io.github.son30kins.workbench.domain.task;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Aggregate task — gồm cả danh sách task con ({@link Subtask}). */
@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Chủ sở hữu. Mọi truy vấn của người dùng đều lọc theo cột này. */
	@Column(nullable = false, updatable = false)
	private UUID userId;

	private String title;

	private String description = "";

	private Instant dueAt;

	@Enumerated(EnumType.STRING)
	private Priority priority = Priority.MEDIUM;

	/** Cột do Postgres tự tính (generated column) — chỉ đọc, dùng để sắp xếp. */
	@Getter(AccessLevel.NONE)
	@Column(insertable = false, updatable = false)
	private Short priorityRank;

	@Enumerated(EnumType.STRING)
	private TaskStatus status = TaskStatus.TODO;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(columnDefinition = "text[]")
	private List<String> tags = new ArrayList<>();

	private UUID templateRunId;

	private String templateName;

	private Instant completedAt;

	/** Quy tắc lặp lại; null = không lặp. Khi hoàn thành, quy tắc chuyển sang task kế tiếp. */
	@JdbcTypeCode(SqlTypes.JSON)
	private Recurrence recurrence;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	/**
	 * EAGER: thẻ task luôn hiện tiến độ task con, và controller đổi entity → DTO ngoài transaction
	 * (open-in-view tắt). Hibernate nạp gộp theo lô (default_batch_fetch_size) nên không bị N+1.
	 */
	@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("position")
	private List<Subtask> subtasks = new ArrayList<>();

	public Task(UUID userId) {
		this.userId = userId;
	}

	/** Gán dữ liệu từ form; trả về true nếu hạn / ưu tiên / trạng thái đổi (cần tính lại lịch nhắc). */
	public boolean apply(TaskData data, Instant now) {
		boolean scheduleChanged = !Objects.equals(dueAt, data.dueAt()) || priority != data.priority()
				|| status != data.status();
		this.title = data.title();
		this.description = data.description();
		this.dueAt = data.dueAt();
		this.priority = data.priority();
		this.tags = new ArrayList<>(data.tags());
		this.recurrence = data.recurrence();
		changeStatus(data.status(), now);
		replaceSubtasks(data.subtasks());
		return scheduleChanged;
	}

	/** Đổi trạng thái; ghi lại lúc hoàn thành khi chuyển sang DONE. */
	public void changeStatus(TaskStatus newStatus, Instant now) {
		if (newStatus == TaskStatus.DONE) {
			if (status != TaskStatus.DONE || completedAt == null) {
				completedAt = now;
			}
		}
		else {
			completedAt = null;
		}
		this.status = newStatus;
	}

	/** Đổi hạn chót (kéo task sang ngày khác trên trang Lịch). */
	public void moveDue(Instant newDue) {
		this.dueAt = newDue;
	}

	public boolean isOpen() {
		return TaskStatus.ACTIVE.contains(status);
	}

	/**
	 * Task lặp lại vừa hoàn thành → dữ liệu cho lần kế tiếp (task con đặt lại chưa xong, quy tắc lặp chuyển theo).
	 * Task này thôi lặp, nên mở lại rồi đánh dấu xong lần nữa không sinh bản trùng.
	 */
	public Optional<TaskData> nextOccurrence(ZoneId zone, Instant now) {
		if (recurrence == null || dueAt == null || status != TaskStatus.DONE) {
			return Optional.empty();
		}
		var next = new TaskData(title, description, recurrence.next(dueAt, zone, now), priority, TaskStatus.TODO, tags,
				subtasks.stream().map(s -> new TaskData.SubtaskData(s.getTitle(), false)).toList(), recurrence);
		recurrence = null;
		return Optional.of(next);
	}

	public void linkTemplateRun(UUID runId, String templateName) {
		this.templateRunId = runId;
		this.templateName = templateName;
	}

	private void replaceSubtasks(List<TaskData.SubtaskData> items) {
		subtasks.clear();
		for (int i = 0; i < items.size(); i++) {
			subtasks.add(new Subtask(this, items.get(i).title(), items.get(i).done(), i));
		}
	}

}
