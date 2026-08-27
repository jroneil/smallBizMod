package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.Permission;
import com.oneilsoftware.sbp.module.usermaintenance.api.PermissionOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownPermissionException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownRoleException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only permission catalog reference (UM-FR-12). The catalog contents are customer-defined;
 * this controller only exposes the reference views, never a write path.
 */
@RestController
public class PermissionController {

	private final PermissionOperations permissionOperations;

	public PermissionController(PermissionOperations permissionOperations) {
		this.permissionOperations = permissionOperations;
	}

	@GetMapping("/api/permissions")
	public List<Permission> list() {
		return permissionOperations.list();
	}

	@GetMapping("/api/roles/{roleId}/permissions")
	public List<Permission> listByRole(@PathVariable UUID roleId) {
		return permissionOperations.listByRole(roleId);
	}

	@GetMapping("/api/permissions/{permissionKey}/roles")
	public List<Role> listRolesByPermission(@PathVariable String permissionKey) {
		return permissionOperations.listRolesByPermission(permissionKey);
	}

	@ExceptionHandler({UnknownPermissionException.class, UnknownRoleException.class})
	ResponseEntity<Void> unknown() {
		return ResponseEntity.notFound().build();
	}
}
