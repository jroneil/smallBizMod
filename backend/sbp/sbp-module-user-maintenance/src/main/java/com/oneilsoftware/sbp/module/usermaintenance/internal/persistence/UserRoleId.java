package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserRoleId implements Serializable {

	private UUID userId;

	private UUID roleId;

	protected UserRoleId() {
	}

	public UserRoleId(UUID userId, UUID roleId) {
		this.userId = userId;
		this.roleId = roleId;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getRoleId() {
		return roleId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UserRoleId that)) {
			return false;
		}
		return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, roleId);
	}
}
