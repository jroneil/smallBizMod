package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.UUID;

public class UnknownRoleException extends RuntimeException {

	public UnknownRoleException(UUID roleId) {
		super("Role does not exist: " + roleId);
	}
}
