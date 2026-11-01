package io.github.son30kins.workbench.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.son30kins.workbench.application.reminder.DigestService;
import io.github.son30kins.workbench.application.reminder.ReminderDeliveryService;
import io.github.son30kins.workbench.application.stats.WeeklyReportService;
import io.github.son30kins.workbench.application.template.TemplateScheduleService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter đầu vào kiểu "đồng hồ": cứ mỗi {@code workbench.jobs.tick} gọi các use case định kỳ.
 * Chỉ chạy khi bật @EnableScheduling ({@code workbench.jobs.enabled=true}, xem AppConfig).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ScheduledJobs {

	private final ReminderDeliveryService reminders;

	private final DigestService digests;

	private final TemplateScheduleService templateSchedules;

	private final WeeklyReportService weeklyReports;

	@Scheduled(fixedDelayString = "${workbench.jobs.tick}", initialDelayString = "5s")
	void deliverReminders() {
		try {
			int count = reminders.deliverDue();
			if (count > 0) {
				log.info("Đã xử lý {} lần nhắc", count);
			}
		}
		catch (RuntimeException ex) {
			log.error("Lỗi job nhắc việc", ex);
		}
	}

	@Scheduled(fixedDelayString = "${workbench.jobs.tick}", initialDelayString = "15s")
	void sendDigests() {
		try {
			int count = digests.sendDue();
			if (count > 0) {
				log.info("Đã gửi {} bản tin", count);
			}
		}
		catch (RuntimeException ex) {
			log.error("Lỗi job bản tin", ex);
		}
	}

	@Scheduled(fixedDelayString = "${workbench.jobs.tick}", initialDelayString = "25s")
	void runTemplateSchedules() {
		try {
			int count = templateSchedules.runDue();
			if (count > 0) {
				log.info("Đã chạy {} lịch form mẫu", count);
			}
		}
		catch (RuntimeException ex) {
			log.error("Lỗi job lịch form mẫu", ex);
		}
	}

	@Scheduled(fixedDelayString = "${workbench.jobs.tick}", initialDelayString = "35s")
	void sendWeeklyReports() {
		try {
			int count = weeklyReports.sendDue();
			if (count > 0) {
				log.info("Đã gửi {} báo cáo tuần", count);
			}
		}
		catch (RuntimeException ex) {
			log.error("Lỗi job báo cáo tuần", ex);
		}
	}

}
