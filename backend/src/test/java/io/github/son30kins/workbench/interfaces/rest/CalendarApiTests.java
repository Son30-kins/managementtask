package io.github.son30kins.workbench.interfaces.rest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import io.github.son30kins.workbench.IntegrationTest;
import io.github.son30kins.workbench.TestFixtures;
import io.github.son30kins.workbench.application.task.TaskService;
import io.github.son30kins.workbench.domain.task.Priority;
import io.github.son30kins.workbench.domain.task.TaskData;
import io.github.son30kins.workbench.domain.user.Role;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static io.github.son30kins.workbench.infrastructure.security.ApiSecurityTests.loggedIn;
import static org.assertj.core.api.Assertions.assertThat;

/** Trang Lịch + link iCal qua HTTP thật. */
@IntegrationTest
@AutoConfigureMockMvc
class CalendarApiTests {

	static final String REQUESTED_BY = "X-Requested-By";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	TestFixtures fixtures;

	@Autowired
	TaskService tasks;

	@Test
	void linkIcal_taiKhongCanDangNhap_taoLaiThiLinkCuHetHieuLuc() throws Exception {
		var user = fixtures.user(Role.USER);
		tasks.create(user.toContext(), TaskData.of("Nộp thuế", Instant.now().plus(Duration.ofDays(2)), Priority.URGENT));

		var url = createLink(user);
		var token = url.substring(url.indexOf("/ical/") + 6, url.length() - 4);
		var feed = mvc.get().uri("/api/calendar/feed/" + token).exchange();
		assertThat(feed).hasStatusOk();
		assertThat(feed).hasContentTypeCompatibleWith("text/calendar");
		assertThat(feed).bodyText().contains("BEGIN:VEVENT", "SUMMARY:🔴 Nộp thuế");

		createLink(user); // tạo lại → link cũ hết hiệu lực
		assertThat(mvc.get().uri("/api/calendar/feed/" + token)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void tatLink_thi404() throws Exception {
		var user = fixtures.user(Role.USER);
		var url = createLink(user);
		mvc.delete().uri("/api/calendar/link").with(loggedIn(user)).header(REQUESTED_BY, "test")
			.assertThat().hasStatus(HttpStatus.NO_CONTENT);
		var token = url.substring(url.indexOf("/ical/") + 6, url.length() - 4);
		assertThat(mvc.get().uri("/api/calendar/feed/" + token)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.get().uri("/api/calendar/feed/khong-ton-tai")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void lich_locTheoKhoangNgay_vaKeoDoiHan_giuGio() {
		var user = fixtures.user(Role.USER);
		var zone = ZoneId.of("Asia/Ho_Chi_Minh");
		var day = LocalDate.now(zone).plusDays(3);
		var due = day.atTime(17, 0).atZone(zone).toInstant();
		var task = tasks.create(user.toContext(), TaskData.of("Gửi báo giá", due, Priority.HIGH));
		tasks.create(user.toContext(), TaskData.of("Xa lắm", due.plus(40, ChronoUnit.DAYS), Priority.LOW));

		mvc.get().uri("/api/tasks/calendar?from=" + day + "&to=" + day.plusDays(6)).with(loggedIn(user))
			.assertThat().hasStatusOk().bodyJson().extractingPath("$[*].title").asArray().containsExactly("Gửi báo giá");

		mvc.patch().uri("/api/tasks/" + task.getId() + "/due").with(loggedIn(user)).header(REQUESTED_BY, "test")
			.contentType(MediaType.APPLICATION_JSON).content("{\"due\":\"" + day.plusDays(1) + "T17:00\"}")
			.assertThat().hasStatusOk();
		assertThat(tasks.get(user.toContext(), task.getId()).orElseThrow().getDueAt())
			.isEqualTo(day.plusDays(1).atTime(17, 0).atZone(zone).toInstant());
	}

	private String createLink(io.github.son30kins.workbench.domain.user.User user) throws Exception {
		var result = mvc.post().uri("/api/calendar/link").with(loggedIn(user)).header(REQUESTED_BY, "test").exchange();
		assertThat(result).hasStatusOk();
		var body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
		var url = body.replaceAll(".*\"url\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		assertThat(url).startsWith("http://localhost:3000/ical/").endsWith(".ics");
		return url;
	}

}
