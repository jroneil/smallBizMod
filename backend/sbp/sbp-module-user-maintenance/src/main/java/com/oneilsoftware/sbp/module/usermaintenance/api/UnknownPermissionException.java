package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.UUID;

public class UnknownPermissionException extends RuntimeException {

	public UnknownPermissionException(String key) {
		super("Permission does not exist: " + key);
	}
}
