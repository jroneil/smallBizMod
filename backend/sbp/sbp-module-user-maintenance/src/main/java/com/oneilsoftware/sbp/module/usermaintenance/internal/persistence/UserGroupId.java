package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserGroupId implements Serializable {

	private UUID userId;

	private UUID groupId;

	protected UserGroupId() {
	}

	public UserGroupId(UUID userId, UUID groupId) {
		this.userId = userId;
		this.groupId = groupId;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getGroupId() {
		return groupId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UserGroupId that)) {
			return false;
		}
		return Objects.equals(userId, that.userId) && Objects.equals(groupId, that.groupId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, groupId);
	}
}
