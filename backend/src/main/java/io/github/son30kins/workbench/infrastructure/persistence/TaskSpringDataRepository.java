package io.github.son30kins.workbench.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.son30kins.workbench.domain.task.Task;
import io.github.son30kins.workbench.domain.task.TaskCounts;
import io.github.son30kins.workbench.domain.task.TaskStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface TaskSpringDataRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

	Optional<Task> findByIdAndUserId(UUID id, UUID userId);

	long deleteByIdAndUserId(UUID id, UUID userId);

	List<Task> findByUserIdAndStatusInAndDueAtBeforeOrderByDueAtAscPriorityRankAsc(UUID userId,
			Collection<TaskStatus> statuses, Instant before);

	List<Task> findByUserIdAndStatusInAndDueAtIsNotNull(UUID userId, Collection<TaskStatus> statuses);

	/** Một truy vấn, 4 con số — dùng constructor expression của JPQL để trả thẳng record. */
	@Query("""
			select new io.github.son30kins.workbench.domain.task.TaskCounts(
			  coalesce(sum(case when t.status in :active then 1 else 0 end), 0),
			  coalesce(sum(case when t.status in :active and t.dueAt >= :today and t.dueAt < :tomorrow then 1 else 0 end), 0),
			  coalesce(sum(case when t.status in :active and t.dueAt < :now then 1 else 0 end), 0),
			  coalesce(sum(case when t.status in :active and t.dueAt >= :today and t.dueAt < :weekEnd then 1 else 0 end), 0))
			from Task t where t.userId = :userId
			""")
	TaskCounts countForPanel(UUID userId, Collection<TaskStatus> active, Instant now, Instant today, Instant tomorrow,
			Instant weekEnd);

	@Query("select t.userId as userId, count(t) as total from Task t group by t.userId")
	List<UserTaskCount> countGroupedByUser();

	/** Interface projection: Spring Data tự map cột alias vào getter. */
	interface UserTaskCount {

		UUID getUserId();

		long getTotal();

	}

	@Query(value = """
			select to_char(date_trunc('week', completed_at at time zone :zone), 'YYYY-MM-DD') as week,
			       count(*) as total,
			       count(*) filter (where due_at is not null and completed_at > due_at) as late
			from tasks
			where user_id = :userId and status = 'DONE' and completed_at >= :from
			group by 1
			""", nativeQuery = true)
	List<WeekRow> completedByWeek(UUID userId, String zone, Instant from);

	@Query(value = """
			select to_char(date_trunc('week', created_at at time zone :zone), 'YYYY-MM-DD') as week,
			       count(*) as total, 0 as late
			from tasks
			where user_id = :userId and created_at >= :from
			group by 1
			""", nativeQuery = true)
	List<WeekRow> createdByWeek(UUID userId, String zone, Instant from);

	@Query(value = """
			select priority, count(*) as open, count(*) filter (where due_at < :now) as overdue
			from tasks
			where user_id = :userId and status in ('TODO', 'IN_PROGRESS')
			group by priority
			""", nativeQuery = true)
	List<PriorityRow> openByPriority(UUID userId, Instant now);

	@Query(value = """
			select count(*) as total,
			       count(*) filter (where due_at is not null) as "withDue",
			       count(*) filter (where due_at is not null and completed_at > due_at) as late
			from tasks
			where user_id = :userId and status = 'DONE' and completed_at >= :from and completed_at < :to
			""", nativeQuery = true)
	CompletionRow completedBetween(UUID userId, Instant from, Instant to);

	long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(UUID userId, Instant from, Instant to);

	interface WeekRow {

		String getWeek();

		long getTotal();

		long getLate();

	}

	interface PriorityRow {

		String getPriority();

		long getOpen();

		long getOverdue();

	}

	interface CompletionRow {

		long getTotal();

		long getWithDue();

		long getLate();

	}

}
