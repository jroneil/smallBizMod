package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.List;
import java.util.UUID;

/**
 * Permission catalog contract. {@code define}, {@code grant}, and {@code revoke} are the
 * customer-facing write surface used to populate the catalog; {@code list}, {@code listByRole},
 * and {@code listRolesByPermission} are the read-only reference surface exposed to the admin view.
 */
public interface PermissionOperations {

	Permission define(String key, String description);

	List<Permission> list();

	List<Permission> listByRole(UUID roleId);

	List<Role> listRolesByPermission(String permissionKey);

	void grant(UUID roleId, String permissionKey);

	void revoke(UUID roleId, String permissionKey);
}
