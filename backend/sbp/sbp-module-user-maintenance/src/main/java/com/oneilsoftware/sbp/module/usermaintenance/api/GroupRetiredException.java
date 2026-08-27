package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.UUID;

public class GroupRetiredException extends RuntimeException {

	public GroupRetiredException(UUID groupId) {
		super("Group is retired and cannot be assigned: " + groupId);
	}
}
