package io.github.son30kins.workbench.interfaces.rest;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.application.stats.StatsService;
import io.github.son30kins.workbench.application.stats.StatsService.Stats;
import io.github.son30kins.workbench.infrastructure.security.CurrentUser;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Số liệu cho trang Thống kê của người dùng đang đăng nhập. */
@RestController
@RequiredArgsConstructor
class StatsController {

	private final StatsService stats;

	@GetMapping("/api/stats")
	Stats stats(CurrentUser user) {
		return stats.stats(user.context());
	}

}
