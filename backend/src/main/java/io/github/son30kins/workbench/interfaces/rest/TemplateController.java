package io.github.son30kins.workbench.interfaces.rest;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.application.i18n.Translator;
import io.github.son30kins.workbench.application.template.TemplateScheduleService;
import io.github.son30kins.workbench.application.template.TemplateService;
import io.github.son30kins.workbench.domain.shared.NotFoundException;
import io.github.son30kins.workbench.domain.template.TemplateDefinition;
import io.github.son30kins.workbench.domain.template.TemplateSchedule;
import io.github.son30kins.workbench.infrastructure.security.CurrentUser;
import io.github.son30kins.workbench.interfaces.rest.dto.request.FormValuesRequest;
import io.github.son30kins.workbench.interfaces.rest.dto.request.ScheduleRequest;
import io.github.son30kins.workbench.interfaces.rest.dto.response.ScheduleResponse;
import io.github.son30kins.workbench.interfaces.rest.dto.response.TemplatePreviewResponse;
import io.github.son30kins.workbench.interfaces.rest.dto.response.TemplateResponse;
import io.github.son30kins.workbench.interfaces.rest.dto.response.TemplateRunHistoryResponse;
import io.github.son30kins.workbench.interfaces.rest.dto.response.TemplateRunResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Form mẫu. Body tạo / sửa dùng thẳng value object {@link TemplateDefinition} của domain (đã có Bean Validation)
 * — tránh viết một DTO trùng y hệt.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class TemplateController {

	private final TemplateService templates;

	private final TemplateScheduleService schedules;

	private final Translator translator;

	@GetMapping("/templates")
	List<TemplateResponse> list(CurrentUser user) {
		var byTemplate = schedules.list(user.id())
			.stream()
			.collect(Collectors.toMap(TemplateSchedule::getTemplateId, Function.identity()));
		return templates.list(user.id())
			.stream()
			.map(t -> TemplateResponse.from(t, byTemplate.get(t.getId())))
			.toList();
	}

	@GetMapping("/templates/{id}")
	TemplateResponse get(CurrentUser user, @PathVariable UUID id) {
		var schedule = schedules.get(user.id(), id).orElse(null);
		return templates.get(user.id(), id)
			.map(t -> TemplateResponse.from(t, schedule))
			.orElseThrow(TemplateController::notFound);
	}

	/** Đặt / đổi lịch tự chạy (giá trị form được kiểm tra ngay). */
	@PutMapping("/templates/{id}/schedule")
	ScheduleResponse saveSchedule(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ScheduleRequest request) {
		return schedules.save(user.context(), id, request.toInput())
			.map(ScheduleResponse::from)
			.orElseThrow(TemplateController::notFound);
	}

	@DeleteMapping("/templates/{id}/schedule")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteSchedule(CurrentUser user, @PathVariable UUID id) {
		schedules.delete(user.id(), id);
	}

	@PostMapping("/templates")
	@ResponseStatus(HttpStatus.CREATED)
	TemplateResponse create(CurrentUser user, @Valid @RequestBody TemplateDefinition definition) {
		return TemplateResponse.from(templates.create(user.id(), definition));
	}

	@PutMapping("/templates/{id}")
	TemplateResponse update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody TemplateDefinition definition) {
		return templates.update(user.id(), id, definition)
			.map(TemplateResponse::from)
			.orElseThrow(TemplateController::notFound);
	}

	@PostMapping("/templates/{id}/duplicate")
	@ResponseStatus(HttpStatus.CREATED)
	TemplateResponse duplicate(CurrentUser user, @PathVariable UUID id) {
		return templates.duplicate(user.context(), id)
			.map(TemplateResponse::from)
			.orElseThrow(TemplateController::notFound);
	}

	@DeleteMapping("/templates/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(CurrentUser user, @PathVariable UUID id) {
		if (!templates.delete(user.id(), id)) {
			throw notFound();
		}
	}

	/** Xem trước danh sách task sẽ tạo — chưa lưu gì. */
	@PostMapping("/templates/{id}/preview")
	TemplatePreviewResponse preview(CurrentUser user, @PathVariable UUID id,
			@Valid @RequestBody FormValuesRequest request) {
		return templates.preview(user.context(), id, request.values())
			.map(plan -> TemplatePreviewResponse.from(plan, plan.warnings().stream().map(translator::get).toList()))
			.orElseThrow(TemplateController::notFound);
	}

	/** Điền form → tạo toàn bộ task. */
	@PostMapping("/templates/{id}/runs")
	@ResponseStatus(HttpStatus.CREATED)
	TemplateRunResponse run(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody FormValuesRequest request) {
		return templates.run(user.context(), id, request.values())
			.map(TemplateRunResponse::new)
			.orElseThrow(TemplateController::notFound);
	}

	@GetMapping("/template-runs")
	List<TemplateRunHistoryResponse> recentRuns(CurrentUser user, @RequestParam(defaultValue = "10") int limit) {
		return templates.recentRuns(user.id(), Math.clamp(limit, 1, 50))
			.stream()
			.map(TemplateRunHistoryResponse::from)
			.toList();
	}

	private static NotFoundException notFound() {
		return new NotFoundException("error.template.notFound");
	}

}
