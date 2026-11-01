package io.github.son30kins.workbench.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.domain.template.TemplateSchedule;
import io.github.son30kins.workbench.domain.template.TemplateScheduleRepository;

import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class TemplateScheduleRepositoryAdapter implements TemplateScheduleRepository {

	private final TemplateScheduleSpringDataRepository jpa;

	@Override
	public TemplateSchedule save(TemplateSchedule schedule) {
		return jpa.save(schedule);
	}

	@Override
	public Optional<TemplateSchedule> findOwned(UUID templateId, UUID userId) {
		return jpa.findByTemplateIdAndUserId(templateId, userId);
	}

	@Override
	public List<TemplateSchedule> findByUser(UUID userId) {
		return jpa.findByUserId(userId);
	}

	@Override
	public boolean deleteOwned(UUID templateId, UUID userId) {
		return jpa.deleteOwned(templateId, userId) > 0;
	}

	@Override
	public List<TemplateSchedule> lockDue(Instant now, int batch) {
		return jpa.lockDue(now, batch);
	}

}
