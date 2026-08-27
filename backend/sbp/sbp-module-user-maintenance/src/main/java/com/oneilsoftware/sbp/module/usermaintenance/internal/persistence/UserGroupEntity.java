package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * User–Group membership join. A row survives group retirement so historical membership records are
 * preserved for audit; removing a membership deletes only this row and never touches the Group.
 */
@Entity
@Table(name = "sbp_user_group")
@IdClass(UserGroupId.class)
public class UserGroupEntity {

	@Id
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Id
	@Column(name = "group_id", nullable = false)
	private UUID groupId;

	@Column(name = "assigned_at", nullable = false)
	private Instant assignedAt;

	protected UserGroupEntity() {
	}

	public UserGroupEntity(UUID userId, UUID groupId, Instant assignedAt) {
		this.userId = userId;
		this.groupId = groupId;
		this.assignedAt = assignedAt;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getGroupId() {
		return groupId;
	}

	public Instant getAssignedAt() {
		return assignedAt;
	}
}
