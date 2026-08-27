package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Group–Role assignment join (UM-FR-11). A row survives role or group retirement so historical
 * grant records are preserved for audit; removing an assignment deletes only this row and never
 * touches the Role or the Group.
 */
@Entity
@Table(name = "sbp_group_role")
@IdClass(GroupRoleId.class)
public class GroupRoleEntity {

	@Id
	@Column(name = "group_id", nullable = false)
	private UUID groupId;

	@Id
	@Column(name = "role_id", nullable = false)
	private UUID roleId;

	@Column(name = "assigned_at", nullable = false)
	private Instant assignedAt;

	protected GroupRoleEntity() {
	}

	public GroupRoleEntity(UUID groupId, UUID roleId, Instant assignedAt) {
		this.groupId = groupId;
		this.roleId = roleId;
		this.assignedAt = assignedAt;
	}

	public UUID getGroupId() {
		return groupId;
	}

	public UUID getRoleId() {
		return roleId;
	}

	public Instant getAssignedAt() {
		return assignedAt;
	}
}
