package com.oneilsoftware.sbp.module.contact.api;

import java.util.UUID;

public class UnknownCustomerException extends RuntimeException {

	public UnknownCustomerException(UUID customerId) {
		super("Customer does not exist: " + customerId);
	}
}
