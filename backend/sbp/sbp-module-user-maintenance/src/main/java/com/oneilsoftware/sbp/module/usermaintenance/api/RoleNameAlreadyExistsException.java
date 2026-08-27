package com.oneilsoftware.sbp.module.usermaintenance.api;

public class RoleNameAlreadyExistsException extends RuntimeException {

	public RoleNameAlreadyExistsException(String name) {
		super("Role name already exists: " + name);
	}
}
