package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.UUID;

public class RoleRetiredException extends RuntimeException {

	public RoleRetiredException(UUID roleId) {
		super("Role is retired and cannot be assigned: " + roleId);
	}
}
