package com.oneilsoftware.sbp.module.usermaintenance.api;

public class GroupNameAlreadyExistsException extends RuntimeException {

	public GroupNameAlreadyExistsException(String name) {
		super("Group name already exists: " + name);
	}
}
