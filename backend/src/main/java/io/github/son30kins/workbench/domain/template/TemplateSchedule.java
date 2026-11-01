package io.github.son30kins.workbench.domain.template;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import io.github.son30kins.workbench.domain.task.Recurrence;

/**
 * Lịch tự chạy của một form mẫu: đến giờ thì app tự điền form bằng giá trị đã lưu và tạo task. Mỗi form mẫu có tối đa
 * một lịch. Trường ngày lưu tương đối theo ngày chạy: {@code "+4"} (DATE) hoặc {@code "+4T17:00"} (DATETIME).
 */
@Entity
@Table(name = "template_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateSchedule {

	private static final Pattern RELATIVE_DATE = Pattern.compile("^([+-]?\\d{1,3})$");

	private static final Pattern RELATIVE_DATETIME = Pattern.compile("^([+-]?\\d{1,3})T(\\d{2}:\\d{2})$");

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID templateId;

	@Column(nullable = false, updatable = false)
	private UUID userId;

	private boolean enabled = true;

	@JdbcTypeCode(SqlTypes.JSON)
	private Recurrence recurrence;

	/** Giờ chạy "HH:mm" theo múi giờ người dùng. */
	private String runTime;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "form_values")
	private Map<String, Object> values = new LinkedHashMap<>();

	private Instant nextRunAt;

	private Instant lastRunAt;

	private String lastError;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	public TemplateSchedule(UUID templateId, UUID userId) {
		this.templateId = templateId;
		this.userId = userId;
	}

	/** Đặt / đổi lịch; bật thì tính lần chạy đầu tiên sau {@code now}. */
	public void configure(boolean enabled, Recurrence recurrence, LocalTime time, Map<String, Object> values,
			ZoneId zone, Instant now) {
		this.enabled = enabled;
		this.recurrence = recurrence;
		this.runTime = time.toString();
		this.values = new LinkedHashMap<>(values);
		this.lastError = null;
		this.nextRunAt = enabled ? recurrence.firstOccurrence(time, zone, now) : null;
	}

	public LocalTime getTime() {
		return LocalTime.parse(runTime);
	}

	/** Lấy ra để chạy: ghi lại lần chạy, hẹn lần sau (bỏ qua các lần đã lỡ). Trả về giờ lẽ ra phải chạy. */
	public Instant claim(ZoneId zone, Instant now) {
		var scheduled = nextRunAt;
		lastRunAt = now;
		nextRunAt = recurrence.next(scheduled, zone, now);
		return scheduled;
	}

	/** Chạy lỗi (giá trị không còn hợp lệ với form) → tắt lịch, chờ người dùng sửa. */
	public void fail(String error) {
		enabled = false;
		nextRunAt = null;
		lastError = error.length() > 500 ? error.substring(0, 500) : error;
	}

	/** Giá trị điền form cho lần chạy vào ngày {@code runDate}. */
	public Map<String, Object> valuesFor(List<TemplateField> fields, LocalDate runDate) {
		return resolve(fields, values, runDate);
	}

	/** Đổi giá trị tương đối của trường ngày ("+4", "+4T17:00") thành ngày cụ thể; giá trị khác giữ nguyên. */
	public static Map<String, Object> resolve(List<TemplateField> fields, Map<String, ?> stored, LocalDate runDate) {
		var result = new LinkedHashMap<String, Object>(stored);
		for (var field : fields) {
			if (!(stored.get(field.key()) instanceof String value)) {
				continue;
			}
			if (field.type() == FieldType.DATE) {
				var m = RELATIVE_DATE.matcher(value.trim());
				if (m.matches()) {
					result.put(field.key(), runDate.plusDays(Integer.parseInt(m.group(1))).toString());
				}
			}
			else if (field.type() == FieldType.DATETIME) {
				var m = RELATIVE_DATETIME.matcher(value.trim());
				if (m.matches()) {
					result.put(field.key(), runDate.plusDays(Integer.parseInt(m.group(1))) + "T" + m.group(2));
				}
			}
		}
		return result;
	}

}
