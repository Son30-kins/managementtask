package io.github.son30kins.workbench.domain.template;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

/** Aggregate form mẫu: các ô cần điền + quy tắc sinh task. {@code fields}/{@code rules} lưu dạng jsonb. */
@Entity
@Table(name = "templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID userId;

	private String name;

	private String description = "";

	@JdbcTypeCode(SqlTypes.JSON)
	private List<TemplateField> fields = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	private List<TemplateRule> rules = new ArrayList<>();

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	public Template(UUID userId, TemplateDefinition definition) {
		this.userId = userId;
		update(definition);
	}

	/** Thay nội dung. Nơi gọi phải kiểm tra trước bằng {@link TemplateValidator}. */
	public void update(TemplateDefinition definition) {
		this.name = definition.name();
		this.description = definition.description();
		this.fields = new ArrayList<>(definition.fields());
		this.rules = new ArrayList<>(definition.rules());
	}

	public TemplateDefinition toDefinition() {
		return new TemplateDefinition(name, description, fields, rules);
	}

	/** Bản sao với tên "… (bản sao)", tối đa 100 ký tự; {@code copySuffix} theo ngôn ngữ người dùng. */
	public TemplateDefinition copyDefinition(String copySuffix) {
		var copyName = name + copySuffix;
		return new TemplateDefinition(copyName.length() > 100 ? copyName.substring(0, 100) : copyName, description,
				fields, rules);
	}

}
