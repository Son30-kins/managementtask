package io.github.son30kins.workbench.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.son30kins.workbench.domain.template.TemplateSchedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface TemplateScheduleSpringDataRepository extends JpaRepository<TemplateSchedule, UUID> {

	Optional<TemplateSchedule> findByTemplateIdAndUserId(UUID templateId, UUID userId);

	List<TemplateSchedule> findByUserId(UUID userId);

	@Modifying
	@Query("delete from TemplateSchedule s where s.templateId = :templateId and s.userId = :userId")
	int deleteOwned(UUID templateId, UUID userId);

	/** {@code FOR UPDATE SKIP LOCKED}: mỗi lịch đến giờ chỉ được một instance lấy ra. */
	@Query(value = """
			select * from template_schedules
			where enabled and next_run_at <= :now
			order by next_run_at
			limit :batch
			for update skip locked
			""", nativeQuery = true)
	List<TemplateSchedule> lockDue(Instant now, int batch);

}
