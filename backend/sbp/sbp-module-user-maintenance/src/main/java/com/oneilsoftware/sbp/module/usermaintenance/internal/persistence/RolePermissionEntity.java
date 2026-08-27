package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "sbp_role_permission")
@IdClass(RolePermissionId.class)
public class RolePermissionEntity {

	@Id
	@Column(name = "role_id", nullable = false)
	private UUID roleId;

	@Id
	@Column(name = "permission_id", nullable = false)
	private UUID permissionId;

	@Column(name = "assigned_at", nullable = false)
	private Instant assignedAt;

	protected RolePermissionEntity() {
	}

	public RolePermissionEntity(UUID roleId, UUID permissionId, Instant assignedAt) {
		this.roleId = roleId;
		this.permissionId = permissionId;
		this.assignedAt = assignedAt;
	}

	public UUID getRoleId() {
		return roleId;
	}

	public UUID getPermissionId() {
		return permissionId;
	}

	public Instant getAssignedAt() {
		return assignedAt;
	}
}
