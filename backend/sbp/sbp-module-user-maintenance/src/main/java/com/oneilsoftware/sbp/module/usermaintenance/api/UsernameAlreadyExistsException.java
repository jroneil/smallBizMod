package com.oneilsoftware.sbp.module.usermaintenance.api;

public class UsernameAlreadyExistsException extends RuntimeException {

	public UsernameAlreadyExistsException(String username) {
		super("Username already exists: " + username);
	}
}
