package io.github.son30kins.workbench.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import io.github.son30kins.workbench.domain.user.Role;
import io.github.son30kins.workbench.domain.user.User;
import io.github.son30kins.workbench.domain.user.UserRepository;

import org.springframework.stereotype.Repository;

/** Adapter: hiện thực port {@link UserRepository} của domain bằng Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepository {

	private final UserSpringDataRepository jpa;

	@Override
	public User save(User user) {
		return jpa.save(user);
	}

	@Override
	public Optional<User> findById(UUID id) {
		return jpa.findById(id);
	}

	@Override
	public Optional<User> findByGoogleSubject(String googleSubject) {
		return jpa.findByGoogleSubject(googleSubject);
	}

	@Override
	public Optional<User> findByCalendarTokenHash(String tokenHash) {
		return jpa.findByCalendarTokenHash(tokenHash);
	}

	@Override
	public boolean existsByEmail(String email) {
		return jpa.existsByEmailIgnoreCase(email);
	}

	@Override
	public long count() {
		return jpa.count();
	}

	@Override
	public long countAdmins() {
		return jpa.countByRole(Role.ADMIN);
	}

	@Override
	public List<User> findActive() {
		return jpa.findByBannedFalse();
	}

	@Override
	public List<User> findAllOrderByCreatedAt() {
		return jpa.findAllByOrderByCreatedAtAsc();
	}

	@Override
	public boolean claimDigest(UUID userId, LocalDate today) {
		return jpa.claimDigest(userId, today) == 1;
	}

	@Override
	public boolean claimWeeklyReport(UUID userId, LocalDate today) {
		return jpa.claimWeeklyReport(userId, today) == 1;
	}

	@Override
	public void lockForProvisioning() {
		jpa.advisoryLockProvisioning();
	}

}
