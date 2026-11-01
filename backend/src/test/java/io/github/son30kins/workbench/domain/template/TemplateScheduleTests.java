package io.github.son30kins.workbench.domain.template;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Trường ngày của lịch tự chạy lưu tương đối theo ngày chạy. */
class TemplateScheduleTests {

	static final List<TemplateField> FIELDS = List.of(
			new TemplateField("ngay_nop", "Hạn nộp", FieldType.DATE, true, List.of(), ""),
			new TemplateField("hop", "Họp", FieldType.DATETIME, false, List.of(), ""),
			new TemplateField("du_an", "Dự án", FieldType.TEXT, true, List.of(), ""));

	@Test
	void doiNgayTuongDoiThanhNgayCuThe() {
		var values = TemplateSchedule.resolve(FIELDS, Map.of("ngay_nop", "+4", "hop", "+1T09:30", "du_an", "+4"),
				LocalDate.of(2026, 11, 2));
		assertThat(values).containsEntry("ngay_nop", "2026-11-06")
			.containsEntry("hop", "2026-11-03T09:30")
			// Trường chữ giữ nguyên dù trông giống số
			.containsEntry("du_an", "+4");
	}

	@Test
	void ngayCuTheGiuNguyen() {
		var values = TemplateSchedule.resolve(FIELDS, Map.of("ngay_nop", "2026-12-24"), LocalDate.of(2026, 11, 2));
		assertThat(values).containsEntry("ngay_nop", "2026-12-24");
	}

}
