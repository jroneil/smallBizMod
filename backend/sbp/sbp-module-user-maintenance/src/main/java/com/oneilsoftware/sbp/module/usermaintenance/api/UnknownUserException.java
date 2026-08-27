package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.UUID;

public class UnknownUserException extends RuntimeException {

	public UnknownUserException(UUID userId) {
		super("User does not exist: " + userId);
	}
}
