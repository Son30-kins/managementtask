package io.github.son30kins.workbench.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.Map;

import io.github.son30kins.workbench.domain.task.Recurrence;
import io.github.son30kins.workbench.domain.template.TemplateSchedule;

public record ScheduleResponse(boolean enabled, Recurrence recurrence, String time, Map<String, Object> values,
		Instant nextRunAt, Instant lastRunAt, String lastError) {

	public static ScheduleResponse from(TemplateSchedule s) {
		return s == null ? null
				: new ScheduleResponse(s.isEnabled(), s.getRecurrence(), s.getRunTime(), s.getValues(), s.getNextRunAt(),
						s.getLastRunAt(), s.getLastError());
	}

}
