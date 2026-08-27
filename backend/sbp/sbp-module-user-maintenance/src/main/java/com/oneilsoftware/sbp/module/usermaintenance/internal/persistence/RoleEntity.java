package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.RoleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sbp_role")
public class RoleEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoleStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RoleEntity() {
	}

	public RoleEntity(
			UUID id,
			String name,
			String description,
			RoleStatus status,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void update(String name, String description, Instant updatedAt) {
		this.name = name;
		this.description = description;
		this.updatedAt = updatedAt;
	}

	public void retire(Instant updatedAt) {
		this.status = RoleStatus.RETIRED;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public RoleStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
