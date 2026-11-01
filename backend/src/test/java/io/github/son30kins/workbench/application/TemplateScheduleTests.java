package io.github.son30kins.workbench.application;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import io.github.son30kins.workbench.IntegrationTest;
import io.github.son30kins.workbench.TestFixtures;
import io.github.son30kins.workbench.application.notification.NotificationService;
import io.github.son30kins.workbench.application.task.TaskService;
import io.github.son30kins.workbench.application.template.TemplateScheduleService;
import io.github.son30kins.workbench.application.template.TemplateScheduleService.ScheduleInput;
import io.github.son30kins.workbench.application.template.TemplateService;
import io.github.son30kins.workbench.domain.task.Recurrence;
import io.github.son30kins.workbench.domain.task.TaskView;
import io.github.son30kins.workbench.domain.template.InvalidFormValuesException;
import io.github.son30kins.workbench.domain.user.Role;
import io.github.son30kins.workbench.domain.user.UserContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Form mẫu tự chạy theo lịch: tới giờ tạo task đúng một lần, giá trị hỏng thì tắt lịch và báo. */
@IntegrationTest
class TemplateScheduleTests {

	@Autowired
	TestFixtures fixtures;

	@Autowired
	TemplateService templates;

	@Autowired
	TemplateScheduleService schedules;

	@Autowired
	TaskService tasks;

	@Autowired
	NotificationService notifications;

	@Autowired
	JdbcTemplate jdbc;

	static final Recurrence MONDAYS = new Recurrence(Recurrence.Frequency.WEEKLY, 1, Set.of(MONDAY), null);

	private record Setup(UserContext owner, java.util.UUID templateId) {
	}

	private Setup weeklyReport() {
		var owner = fixtures.context(Role.USER);
		templates.seedDefaults(owner.id(), owner.language());
		var report = templates.list(owner.id()).stream().filter(t -> t.getName().equals("Báo cáo tuần")).findFirst().orElseThrow();
		return new Setup(owner, report.getId());
	}

	@Test
	void denGio_taoTask_motLan_vaHenLanSau() {
		var s = weeklyReport();
		var saved = schedules.save(s.owner(), s.templateId(), new ScheduleInput(true, MONDAYS, LocalTime.of(8, 0),
				Map.of("tuan", "45", "ngay_nop", "+4", "du_an", "Beta", "gui_sep", true)))
			.orElseThrow();
		assertThat(saved.getNextRunAt()).isAfter(Instant.now());

		// Giả lập đã tới giờ chạy
		jdbc.update("update template_schedules set next_run_at = now() - interval '1 minute' where template_id = ?",
				s.templateId());
		schedules.runDue();
		schedules.runDue(); // lần 2 không chạy lại

		assertThat(tasks.list(s.owner(), TaskView.ALL, "tuần 45")).hasSize(3);
		assertThat(notifications.inbox(s.owner().id()).items()).extracting("title")
			.containsExactly("Đã tự tạo 3 task từ form mẫu “Báo cáo tuần”");
		var after = schedules.get(s.owner().id(), s.templateId()).orElseThrow();
		assertThat(after.getNextRunAt()).isAfter(Instant.now());
		assertThat(after.getLastRunAt()).isNotNull();
	}

	@Test
	void giaTriSai_baoLoiNgayLucLuu() {
		var s = weeklyReport();
		assertThatThrownBy(() -> schedules.save(s.owner(), s.templateId(),
				new ScheduleInput(true, MONDAYS, LocalTime.of(8, 0), Map.of("tuan", "45", "ngay_nop", "+4"))))
			.isInstanceOf(InvalidFormValuesException.class);
	}

	@Test
	void giaTriHetHopLe_tatLich_vaBaoNguoiDung() {
		var s = weeklyReport();
		schedules.save(s.owner(), s.templateId(), new ScheduleInput(true, MONDAYS, LocalTime.of(8, 0),
				Map.of("tuan", "45", "ngay_nop", "+4", "du_an", "Gamma")));
		// Giá trị đã lưu bị hỏng (như khi form mẫu được sửa thêm trường bắt buộc)
		jdbc.update("""
				update template_schedules set form_values = '{"tuan": "45"}'::jsonb, next_run_at = now() - interval '1 minute'
				where template_id = ?""", s.templateId());
		schedules.runDue();

		var after = schedules.get(s.owner().id(), s.templateId()).orElseThrow();
		assertThat(after.isEnabled()).isFalse();
		assertThat(after.getLastError()).isNotBlank();
		assertThat(tasks.list(s.owner(), TaskView.ALL, "Gamma")).isEmpty();
		assertThat(notifications.inbox(s.owner().id()).items()).extracting("title")
			.containsExactly("Form mẫu “Báo cáo tuần” không tự chạy được — đã tắt lịch");
	}

}
