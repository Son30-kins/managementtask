package io.github.son30kins.workbench.interfaces.rest;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.application.calendar.CalendarFeedService;
import io.github.son30kins.workbench.domain.shared.NotFoundException;
import io.github.son30kins.workbench.infrastructure.security.CurrentUser;
import io.github.son30kins.workbench.interfaces.rest.dto.response.LinkResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Link lịch iCal: bật / tạo lại / tắt (cần đăng nhập) và nội dung lịch (công khai, xác thực bằng token). */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
class CalendarController {

	private static final MediaType ICS = new MediaType("text", "calendar", java.nio.charset.StandardCharsets.UTF_8);

	private final CalendarFeedService feeds;

	record CalendarStatus(boolean enabled) {
	}

	@GetMapping
	CalendarStatus status(CurrentUser user) {
		return new CalendarStatus(feeds.enabled(user.id()));
	}

	/** Tạo link mới (link cũ hết hiệu lực). URL chỉ hiện đúng lần này. */
	@PostMapping("/link")
	LinkResponse createLink(CurrentUser user) {
		return new LinkResponse(feeds.createLink(user.id()));
	}

	@DeleteMapping("/link")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void revoke(CurrentUser user) {
		feeds.revoke(user.id());
	}

	@GetMapping("/feed/{token}")
	ResponseEntity<String> feed(@PathVariable String token) {
		var body = feeds.feed(token).orElseThrow(() -> new NotFoundException("error.calendar.notFound"));
		return ResponseEntity.ok()
			.contentType(ICS)
			.cacheControl(CacheControl.noStore())
			.header("X-Robots-Tag", "noindex")
			.body(body);
	}

}
