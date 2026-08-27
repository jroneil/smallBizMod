package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupOperations {

	Group create(CreateGroup command);

	List<Group> list();

	Optional<Group> findById(UUID groupId);

	Group update(UUID groupId, UpdateGroup command);

	Group retire(UUID groupId);

	List<User> listMembers(UUID groupId);

	/**
	 * Roles assigned directly to the group (any role status). Retired roles remain listed for
	 * history; only {@link RoleStatus#ACTIVE} roles flow into effective-role calculation.
	 */
	List<Role> listRoles(UUID groupId);

	/**
	 * Assigns an ACTIVE role to an ACTIVE group (idempotent). A RETIRED role or a RETIRED group is
	 * rejected, matching the User↔Role and User↔Group assignment rules.
	 */
	List<Role> assignRole(UUID groupId, UUID roleId);

	/**
	 * Removes the group→role assignment, preserving the Role and Group records and any other
	 * assignments. Removing a relationship is always permitted, including for retired roles/groups.
	 */
	List<Role> removeRole(UUID groupId, UUID roleId);
}
