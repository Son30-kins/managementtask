package io.github.son30kins.workbench.application.stats;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.domain.shared.TimeZones;
import io.github.son30kins.workbench.domain.task.Priority;
import io.github.son30kins.workbench.domain.task.TaskRepository;
import io.github.son30kins.workbench.domain.task.TaskStatistics;
import io.github.son30kins.workbench.domain.user.UserContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trang Thống kê: việc xong theo tuần (đúng hạn / trễ), việc tạo mới, tỉ lệ đúng hạn, việc đang mở theo ưu tiên. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsService {

	/** Số tuần hiện trên biểu đồ (kể cả tuần này). */
	public static final int WEEKS = 8;

	private final TaskRepository tasks;

	private final Clock clock;

	public record WeekStat(LocalDate start, long onTime, long late, long created) {
	}

	public record PriorityStat(Priority priority, long open, long overdue) {
	}

	/**
	 * @param completed7 việc xong trong 7 ngày qua
	 * @param completedPrev7 việc xong trong 7 ngày trước đó (để so sánh)
	 * @param onTimeRate30 % xong đúng hạn trong 30 ngày qua; null khi chưa có việc có hạn nào xong
	 */
	public record Stats(List<WeekStat> weeks, long completed7, long completedPrev7, Integer onTimeRate30, long open,
			long overdue, List<PriorityStat> byPriority) {
	}

	public Stats stats(UserContext owner) {
		var now = clock.instant();
		var zone = owner.zone();
		var thisMonday = TimeZones.localDate(now, zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		var firstMonday = thisMonday.minusWeeks(WEEKS - 1);
		var from = TimeZones.fromLocal(firstMonday, LocalTime.MIDNIGHT, zone);

		var completed = byWeek(tasks.completedByWeek(owner.id(), zone, from));
		var created = byWeek(tasks.createdByWeek(owner.id(), zone, from));
		var weeks = firstMonday.datesUntil(thisMonday.plusDays(1), Period.ofWeeks(1)).map(monday -> {
			var done = completed.get(monday);
			var made = created.get(monday);
			long total = done == null ? 0 : done.total();
			long late = done == null ? 0 : done.late();
			return new WeekStat(monday, total - late, late, made == null ? 0 : made.total());
		}).toList();

		var open = tasks.openByPriority(owner.id(), now)
			.stream()
			.collect(Collectors.toMap(TaskStatistics.PriorityCount::priority, Function.identity()));
		var byPriority = Arrays.stream(Priority.values()).map(p -> {
			var count = open.get(p);
			return new PriorityStat(p, count == null ? 0 : count.open(), count == null ? 0 : count.overdue());
		}).toList();

		var week = Duration.ofDays(7);
		return new Stats(weeks, tasks.completedBetween(owner.id(), now.minus(week), now).total(),
				tasks.completedBetween(owner.id(), now.minus(week.multipliedBy(2)), now.minus(week)).total(),
				tasks.completedBetween(owner.id(), now.minus(Duration.ofDays(30)), now).onTimeRate(),
				byPriority.stream().mapToLong(PriorityStat::open).sum(),
				byPriority.stream().mapToLong(PriorityStat::overdue).sum(), byPriority);
	}

	private static Map<LocalDate, TaskStatistics.WeekCount> byWeek(List<TaskStatistics.WeekCount> rows) {
		return rows.stream().collect(Collectors.toMap(TaskStatistics.WeekCount::weekStart, Function.identity()));
	}

	/** Mốc dùng chung: 0h thứ 2 tuần này theo giờ địa phương. */
	static Instant startOfWeek(Instant now, ZoneId zone) {
		var monday = TimeZones.localDate(now, zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		return TimeZones.fromLocal(monday, LocalTime.MIDNIGHT, zone);
	}

}
