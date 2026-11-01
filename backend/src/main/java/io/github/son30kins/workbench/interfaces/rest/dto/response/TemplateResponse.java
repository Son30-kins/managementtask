package io.github.son30kins.workbench.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.son30kins.workbench.domain.template.Template;
import io.github.son30kins.workbench.domain.template.TemplateField;
import io.github.son30kins.workbench.domain.template.TemplateRule;
import io.github.son30kins.workbench.domain.template.TemplateSchedule;

public record TemplateResponse(UUID id, String name, String description, List<TemplateField> fields,
		List<TemplateRule> rules, Instant createdAt, Instant updatedAt, ScheduleResponse schedule) {

	public static TemplateResponse from(Template t) {
		return from(t, null);
	}

	/** @param schedule lịch tự chạy (null = chưa đặt) */
	public static TemplateResponse from(Template t, TemplateSchedule schedule) {
		return new TemplateResponse(t.getId(), t.getName(), t.getDescription(), t.getFields(), t.getRules(),
				t.getCreatedAt(), t.getUpdatedAt(), ScheduleResponse.from(schedule));
	}

}
