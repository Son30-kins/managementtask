package io.github.son30kins.workbench.application.stats;

import java.time.Duration;
import java.time.Instant;

import io.github.son30kins.workbench.IntegrationTest;
import io.github.son30kins.workbench.TestFixtures;
import io.github.son30kins.workbench.application.task.TaskService;
import io.github.son30kins.workbench.domain.shared.Language;
import io.github.son30kins.workbench.domain.task.Priority;
import io.github.son30kins.workbench.domain.task.TaskData;
import io.github.son30kins.workbench.domain.user.Role;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Thống kê theo tuần (giờ địa phương) và nội dung báo cáo tuần. */
@IntegrationTest
class StatsTests {

	@Autowired
	TestFixtures fixtures;

	@Autowired
	TaskService tasks;

	@Autowired
	StatsService stats;

	@Autowired
	WeeklyReportService reports;

	@Autowired
	JdbcTemplate jdbc;

	/** Task đã xong {@code daysAgo} ngày trước; trễ hạn nếu {@code late}. */
	private void done(java.util.UUID userId, io.github.son30kins.workbench.domain.user.UserContext owner, String title,
			int daysAgo, boolean late) {
		var task = tasks.create(owner, TaskData.of(title, Instant.now().plus(Duration.ofDays(1)), Priority.HIGH));
		jdbc.update("""
				update tasks set status = 'DONE', completed_at = now() - make_interval(days => ?),
				  due_at = now() - make_interval(days => ?) + (case when ? then interval '-1 hour' else interval '1 hour' end)
				where id = ?""", daysAgo, daysAgo, late, task.getId());
	}

	@Test
	void thongKe_8Tuan_dungHan_treHan_vaViecDangMo() {
		var user = fixtures.user(Role.USER);
		var owner = user.toContext();
		done(user.getId(), owner, "Xong đúng hạn", 0, false);
		done(user.getId(), owner, "Xong trễ", 0, true);
		done(user.getId(), owner, "Tuần trước", 7, false);
		tasks.create(owner, TaskData.of("Đang quá hạn", Instant.now().minus(Duration.ofHours(2)), Priority.URGENT));
		tasks.create(owner, TaskData.of("Còn hạn", Instant.now().plus(Duration.ofDays(3)), Priority.LOW));

		var s = stats.stats(owner);

		assertThat(s.weeks()).hasSize(StatsService.WEEKS);
		var thisWeek = s.weeks().getLast();
		assertThat(thisWeek.onTime() + thisWeek.late()).isEqualTo(2);
		assertThat(thisWeek.late()).isEqualTo(1);
		assertThat(s.weeks().stream().mapToLong(w -> w.onTime() + w.late()).sum()).isEqualTo(3);
		assertThat(s.completed7()).isEqualTo(2);
		assertThat(s.onTimeRate30()).isEqualTo(67);
		assertThat(s.open()).isEqualTo(2);
		assertThat(s.overdue()).isEqualTo(1);
		assertThat(s.byPriority()).extracting("priority").containsExactly(Priority.URGENT, Priority.HIGH, Priority.MEDIUM,
				Priority.LOW);
	}

	@Test
	void chuaCoViecCoHan_thiKhongCoTiLe() {
		var owner = fixtures.user(Role.USER).toContext();
		assertThat(stats.stats(owner).onTimeRate30()).isNull();
	}

	@Test
	void baoCaoTuan_tomTatTuanTruoc_theoNgonNgu() {
		var user = fixtures.user(Role.USER, Language.EN);
		var owner = user.toContext();
		// Thứ 2 tuần này 00:00 + 3 ngày trước → chắc chắn thuộc tuần trước
		var lastWeek = StatsService.startOfWeek(Instant.now(), owner.zone()).minus(Duration.ofDays(3));
		var task = tasks.create(owner, TaskData.of("Ship it", lastWeek.plus(Duration.ofHours(1)), Priority.HIGH));
		jdbc.update("update tasks set status = 'DONE', completed_at = ?, created_at = ? where id = ?",
				java.sql.Timestamp.from(lastWeek), java.sql.Timestamp.from(lastWeek.minus(Duration.ofDays(1))), task.getId());

		var report = reports.build(user, Instant.now());

		assertThat(report).isNotNull();
		assertThat(report.title()).startsWith("Weekly report ");
		assertThat(report.html()).contains("Completed: 1 task (1 on time, 0 late)", "New: 1 task", "1 more task than");
		assertThat(report.summary()).isEqualTo("Completed 1 (1 on time) · 0 overdue");
	}

	@Test
	void tuanKhongCoGi_thiKhongGui() {
		var user = fixtures.user(Role.USER);
		assertThat(reports.build(user, Instant.now())).isNull();
	}

}
