package io.github.son30kins.workbench.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import io.github.son30kins.workbench.IntegrationTest;
import io.github.son30kins.workbench.TestFixtures;
import io.github.son30kins.workbench.application.task.TaskService;
import io.github.son30kins.workbench.domain.shared.ValidationException;
import io.github.son30kins.workbench.domain.task.Priority;
import io.github.son30kins.workbench.domain.task.Recurrence;
import io.github.son30kins.workbench.domain.task.TaskData;
import io.github.son30kins.workbench.domain.task.TaskStatus;
import io.github.son30kins.workbench.domain.task.TaskView;
import io.github.son30kins.workbench.domain.user.Role;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Hoàn thành task lặp lại → lần kế tiếp được tạo đúng một lần, task con đặt lại. */
@IntegrationTest
class RecurringTaskTests {

	@Autowired
	TestFixtures fixtures;

	@Autowired
	TaskService tasks;

	static TaskData weekly(String title, Instant due) {
		return new TaskData(title, "ghi chú", due, Priority.HIGH, TaskStatus.TODO, List.of("định kỳ"),
				List.of(new TaskData.SubtaskData("Bước 1", true), new TaskData.SubtaskData("Bước 2", false)),
				new Recurrence(Recurrence.Frequency.WEEKLY, 1, Set.of(), null));
	}

	@Test
	void hoanThanh_taoLanKeTiep_motLanDuyNhat() {
		var owner = fixtures.context(Role.USER);
		var due = Instant.now().plus(Duration.ofDays(1));
		var task = tasks.create(owner, weekly("Tưới cây", due));

		tasks.changeStatus(owner, task.getId(), TaskStatus.DONE);

		var open = tasks.list(owner, TaskView.ALL, "Tưới cây").stream().filter(t -> t.isOpen()).toList();
		assertThat(open).singleElement().satisfies(next -> {
			assertThat(next.getDueAt()).isEqualTo(due.plus(Duration.ofDays(7)));
			assertThat(next.getRecurrence()).isNotNull();
			assertThat(next.getSubtasks()).extracting("done").containsExactly(false, false);
			assertThat(next.getTags()).containsExactly("định kỳ");
			assertThat(next.getPriority()).isEqualTo(Priority.HIGH);
		});
		assertThat(tasks.get(owner, task.getId()).orElseThrow().getRecurrence()).isNull();

		// Mở lại rồi xong lần nữa → không sinh bản trùng
		tasks.changeStatus(owner, task.getId(), TaskStatus.TODO);
		tasks.changeStatus(owner, task.getId(), TaskStatus.DONE);
		assertThat(tasks.list(owner, TaskView.ALL, "Tưới cây").stream().filter(t -> t.isOpen()).count()).isEqualTo(1);
	}

	@Test
	void huyTask_khongTaoLanKeTiep() {
		var owner = fixtures.context(Role.USER);
		var task = tasks.create(owner, weekly("Dừng lặp", Instant.now().plus(Duration.ofDays(1))));
		tasks.changeStatus(owner, task.getId(), TaskStatus.CANCELLED);
		assertThat(tasks.list(owner, TaskView.ALL, "Dừng lặp").stream().filter(t -> t.isOpen())).isEmpty();
	}

	@Test
	void lapLai_canHanChot() {
		var owner = fixtures.context(Role.USER);
		assertThatThrownBy(() -> tasks.create(owner, weekly("Không hạn", null))).isInstanceOf(ValidationException.class);
	}

}
