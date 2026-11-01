package io.github.son30kins.workbench.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.domain.task.Task;
import io.github.son30kins.workbench.domain.task.TaskCounts;
import io.github.son30kins.workbench.domain.task.TaskRepository;
import io.github.son30kins.workbench.domain.task.TaskSearch;
import io.github.son30kins.workbench.domain.task.TaskStatus;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Adapter cho task. {@link #search} dịch bộ lọc {@link TaskSearch} của domain thành JPA Specification
 * (Criteria API) — domain không cần biết SQL hay Spring Data.
 */
@Repository
@RequiredArgsConstructor
class TaskRepositoryAdapter implements TaskRepository {

	private static final Sort BOARD_ORDER = Sort.by(Sort.Order.asc("priorityRank"), Sort.Order.asc("dueAt").nullsLast(),
			Sort.Order.desc("createdAt"));

	private static final Sort COMPLETED_ORDER = Sort.by(Sort.Order.desc("completedAt"));

	private static final Sort DUE_ORDER = Sort.by(Sort.Order.asc("dueAt"), Sort.Order.asc("priorityRank"));

	private final TaskSpringDataRepository jpa;

	@Override
	public Task save(Task task) {
		return jpa.save(task);
	}

	@Override
	public Optional<Task> findById(UUID id) {
		return jpa.findById(id);
	}

	@Override
	public Optional<Task> findOwned(UUID id, UUID userId) {
		return jpa.findByIdAndUserId(id, userId);
	}

	@Override
	public boolean deleteOwned(UUID id, UUID userId) {
		return jpa.deleteByIdAndUserId(id, userId) > 0;
	}

	@Override
	public List<Task> search(TaskSearch search) {
		var spec = toSpecification(search);
		var sort = switch (search.sort()) {
			case BOARD -> BOARD_ORDER;
			case RECENTLY_COMPLETED -> COMPLETED_ORDER;
			case DUE -> DUE_ORDER;
		};
		if (search.limit() <= 0) {
			return jpa.findAll(spec, sort);
		}
		return jpa.findBy(spec, query -> query.sortBy(sort).limit(search.limit()).all());
	}

	@Override
	public List<Task> findOpenDueBefore(UUID userId, Instant before) {
		return jpa.findByUserIdAndStatusInAndDueAtBeforeOrderByDueAtAscPriorityRankAsc(userId, TaskStatus.ACTIVE, before);
	}

	@Override
	public List<Task> findOpenWithDueDate(UUID userId) {
		return jpa.findByUserIdAndStatusInAndDueAtIsNotNull(userId, TaskStatus.ACTIVE);
	}

	@Override
	public TaskCounts countForPanel(UUID userId, Instant now, Instant today, Instant tomorrow, Instant weekEnd) {
		return jpa.countForPanel(userId, TaskStatus.ACTIVE, now, today, tomorrow, weekEnd);
	}

	@Override
	public Map<UUID, Long> countByUser() {
		return jpa.countGroupedByUser()
			.stream()
			.collect(Collectors.toMap(TaskSpringDataRepository.UserTaskCount::getUserId,
					TaskSpringDataRepository.UserTaskCount::getTotal));
	}

	private static Specification<Task> toSpecification(TaskSearch search) {
		Specification<Task> spec = (root, query, cb) -> cb.and(cb.equal(root.get("userId"), search.userId()),
				root.get("status").in(search.statuses()));
		if (search.dueFrom() != null) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueAt"), search.dueFrom()));
		}
		if (search.dueBefore() != null) {
			spec = spec.and((root, query, cb) -> cb.lessThan(root.get("dueAt"), search.dueBefore()));
		}
		if (search.text() != null) {
			// Escape % _ \ để người dùng gõ ký tự đặc biệt không thành wildcard
			var pattern = "%" + search.text().toLowerCase(Locale.ROOT).replaceAll("([%_\\\\])", "\\\\$1") + "%";
			spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
					cb.like(cb.lower(root.get("description")), pattern, '\\')));
		}
		return spec;
	}

}
