package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.UUID;

public class UnknownGroupException extends RuntimeException {

	public UnknownGroupException(UUID groupId) {
		super("Group does not exist: " + groupId);
	}
}
