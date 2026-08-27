package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserOperations {

	User create(CreateUser command);

	BulkUserImportResult bulkCreate(List<BulkCreateUser> commands);

	List<User> search(UserFilter filter);

	Optional<User> findById(UUID userId);

	Optional<User> findByUsername(String username);

	User update(UUID userId, UpdateUser command);

	User activate(UUID userId);

	User deactivate(UUID userId);

	List<Role> listRoles(UUID userId);

	List<Role> assignRole(UUID userId, UUID roleId);

	List<Role> removeRole(UUID userId, UUID roleId);

	/**
	 * Effective roles for the user: the union of directly assigned ACTIVE roles and ACTIVE roles
	 * inherited through the user's ACTIVE group memberships, deduplicated and ordered by name.
	 * RETIRED roles and grants from RETIRED groups are retained as historical assignments but are
	 * not part of the effective view. This is role-resolution data, not authorization enforcement.
	 */
	List<Role> listEffectiveRoles(UUID userId);

	List<Group> listGroups(UUID userId);

	List<Group> assignGroup(UUID userId, UUID groupId);

	List<Group> removeGroup(UUID userId, UUID groupId);
}
