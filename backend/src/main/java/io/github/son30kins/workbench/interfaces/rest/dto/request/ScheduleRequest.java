package io.github.son30kins.workbench.interfaces.rest.dto.request;

import java.time.LocalTime;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import io.github.son30kins.workbench.application.template.TemplateScheduleService.ScheduleInput;
import io.github.son30kins.workbench.domain.task.Recurrence;
import io.github.son30kins.workbench.domain.user.UserSettings;

/** Body PUT /api/templates/{id}/schedule. Trường ngày trong {@code values}: "+4" hoặc "+4T17:00" (so với ngày chạy). */
public record ScheduleRequest(boolean enabled, @NotNull @Valid Recurrence recurrence,
		@NotNull @Pattern(regexp = UserSettings.HHMM, message = "{validation.time}") String time,
		Map<String, Object> values) {

	public ScheduleInput toInput() {
		return new ScheduleInput(enabled, recurrence, LocalTime.parse(time), values == null ? Map.of() : values);
	}

}
