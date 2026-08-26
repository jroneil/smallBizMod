package com.oneilsoftware.sbp.module.customer.api;

public record CreateCustomer(String name) {

	public CreateCustomer {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Customer name is required");
		}
		name = name.trim();
	}
}
