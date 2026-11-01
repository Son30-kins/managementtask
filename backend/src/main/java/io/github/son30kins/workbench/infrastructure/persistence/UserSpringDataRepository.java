package io.github.son30kins.workbench.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.son30kins.workbench.domain.user.Role;
import io.github.son30kins.workbench.domain.user.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Spring Data tự sinh câu truy vấn từ tên phương thức (derived query). Chỉ adapter dùng interface này. */
interface UserSpringDataRepository extends JpaRepository<User, UUID> {

	Optional<User> findByGoogleSubject(String googleSubject);

	Optional<User> findByCalendarTokenHash(String calendarTokenHash);

	boolean existsByEmailIgnoreCase(String email);

	long countByRole(Role role);

	List<User> findByBannedFalse();

	List<User> findAllByOrderByCreatedAtAsc();

	@Modifying
	@Query("""
			update User u set u.lastDigestOn = :today
			where u.id = :userId and (u.lastDigestOn is null or u.lastDigestOn <> :today)
			""")
	int claimDigest(UUID userId, LocalDate today);

	@Modifying
	@Query("""
			update User u set u.lastWeeklyReportOn = :today
			where u.id = :userId and (u.lastWeeklyReportOn is null or u.lastWeeklyReportOn <> :today)
			""")
	int claimWeeklyReport(UUID userId, LocalDate today);

	/** pg_advisory_xact_lock trả về void → bọc trong select để Hibernate đọc được kết quả. */
	@Query(value = "select 1 from (select pg_advisory_xact_lock(72710302)) l", nativeQuery = true)
	Integer advisoryLockProvisioning();

}
