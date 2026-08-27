package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * User–Role assignment join. A row survives role retirement so historical assignment records are
 * preserved for audit; removing an assignment deletes only this row and never touches the Role.
 */
@Entity
@Table(name = "sbp_user_role")
@IdClass(UserRoleId.class)
public class UserRoleEntity {

	@Id
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Id
	@Column(name = "role_id", nullable = false)
	private UUID roleId;

	@Column(name = "assigned_at", nullable = false)
	private Instant assignedAt;

	protected UserRoleEntity() {
	}

	public UserRoleEntity(UUID userId, UUID roleId, Instant assignedAt) {
		this.userId = userId;
		this.roleId = roleId;
		this.assignedAt = assignedAt;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getRoleId() {
		return roleId;
	}

	public Instant getAssignedAt() {
		return assignedAt;
	}
}
