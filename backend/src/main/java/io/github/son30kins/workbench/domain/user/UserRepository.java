package io.github.son30kins.workbench.domain.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (cổng) lưu trữ người dùng — domain chỉ khai báo những gì nghiệp vụ cần.
 * Hiện thực ở {@code infrastructure.persistence.UserRepositoryAdapter} bằng Spring Data JPA.
 */
public interface UserRepository {

	User save(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByGoogleSubject(String googleSubject);

	Optional<User> findByCalendarTokenHash(String tokenHash);

	boolean existsByEmail(String email);

	long count();

	long countAdmins();

	/** Người dùng chưa bị khoá — cho job bản tin. */
	List<User> findActive();

	List<User> findAllOrderByCreatedAt();

	/**
	 * Đánh dấu đã gửi bản tin ngày {@code today}. Chỉ thành công 1 lần mỗi ngày kể cả khi chạy nhiều instance.
	 * @return true nếu vừa đánh dấu
	 */
	boolean claimDigest(UUID userId, LocalDate today);

	/** Đánh dấu đã gửi báo cáo tuần hôm nay; false nếu instance khác đã gửi. */
	boolean claimWeeklyReport(UUID userId, LocalDate today);

	/** Tuần tự hoá việc tạo tài khoản (hai người đăng nhập cùng lúc không cùng thành admin đầu tiên). */
	void lockForProvisioning();

}
