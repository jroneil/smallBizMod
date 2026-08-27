package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class GroupRoleId implements Serializable {

	private UUID groupId;

	private UUID roleId;

	protected GroupRoleId() {
	}

	public GroupRoleId(UUID groupId, UUID roleId) {
		this.groupId = groupId;
		this.roleId = roleId;
	}

	public UUID getGroupId() {
		return groupId;
	}

	public UUID getRoleId() {
		return roleId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof GroupRoleId that)) {
			return false;
		}
		return Objects.equals(groupId, that.groupId) && Objects.equals(roleId, that.roleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, roleId);
	}
}
