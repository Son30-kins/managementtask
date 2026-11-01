package io.github.son30kins.workbench.domain.user;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import io.github.son30kins.workbench.domain.shared.Language;

/**
 * Aggregate người dùng — tạo lần đầu khi đăng nhập Google. Trạng thái chỉ đổi qua các phương thức
 * nghiệp vụ (ban, changeRole…) chứ không có setter tự do.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA cần constructor rỗng
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Claim "sub" của Google — định danh không đổi kể cả khi người dùng đổi email. */
	private String googleSubject;

	private String email;

	private String name;

	private String image;

	@Enumerated(EnumType.STRING)
	private Role role = Role.USER;

	private boolean banned;

	@Getter(AccessLevel.NONE)
	private String timezone;

	@Getter(AccessLevel.NONE)
	@JdbcTypeCode(SqlTypes.JSON)
	private UserSettings settings;

	/** Ngôn ngữ giao diện, thông báo và tin Telegram. */
	private Language language = Language.DEFAULT;

	private LocalDate lastDigestOn;

	/** Ngày (thứ 2, giờ địa phương) đã gửi báo cáo tuần gần nhất — chống gửi trùng. */
	private LocalDate lastWeeklyReportOn;

	/** SHA-256 của token trong link lịch iCal; null = chưa bật link. */
	private String calendarTokenHash;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	public User(String googleSubject, String email, String name, String image, Role role, ZoneId timezone,
			Language language) {
		this.googleSubject = googleSubject;
		this.email = email;
		this.name = name;
		this.image = image;
		this.role = role;
		this.timezone = timezone.getId();
		this.language = language;
	}

	public void updateProfile(String email, String name, String image) {
		this.email = email;
		this.name = name;
		this.image = image;
	}

	public void updatePreferences(ZoneId timezone, UserSettings settings) {
		this.timezone = timezone.getId();
		this.settings = settings;
	}

	public void changeLanguage(Language language) {
		this.language = language;
	}

	/** Đặt / thu hồi link lịch iCal (chỉ lưu hash của token). */
	public void changeCalendarToken(String tokenHash) {
		this.calendarTokenHash = tokenHash;
	}

	public void ban() {
		this.banned = true;
	}

	public void unban() {
		this.banned = false;
	}

	public void changeRole(Role role) {
		this.role = role;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	public ZoneId getZone() {
		return ZoneId.of(timezone);
	}

	/** Cài đặt đã lưu, hoặc mặc định nếu chưa lưu. */
	public UserSettings getEffectiveSettings() {
		return UserSettings.orDefaults(settings);
	}

	public UserContext toContext() {
		return new UserContext(id, getZone(), getEffectiveSettings(), language);
	}

}
