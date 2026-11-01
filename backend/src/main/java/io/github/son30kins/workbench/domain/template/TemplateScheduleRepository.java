package io.github.son30kins.workbench.domain.template;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port lưu lịch tự chạy của form mẫu. Phương thức đọc / sửa của người dùng đều lọc theo chủ sở hữu. */
public interface TemplateScheduleRepository {

	TemplateSchedule save(TemplateSchedule schedule);

	Optional<TemplateSchedule> findOwned(UUID templateId, UUID userId);

	List<TemplateSchedule> findByUser(UUID userId);

	boolean deleteOwned(UUID templateId, UUID userId);

	/** Các lịch đã đến giờ, khoá dòng (instance khác bỏ qua) — dùng trong transaction rồi gọi {@code claim}. */
	List<TemplateSchedule> lockDue(Instant now, int batch);

}
