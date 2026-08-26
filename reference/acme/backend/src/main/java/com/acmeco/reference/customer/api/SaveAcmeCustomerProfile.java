package com.acmeco.reference.customer.api;

public record SaveAcmeCustomerProfile(
		String serviceRegion,
		String accountNotes) {

	public SaveAcmeCustomerProfile {
		if (serviceRegion == null || serviceRegion.isBlank()) {
			throw new IllegalArgumentException("Service region is required");
		}
		if (serviceRegion.length() > 100) {
			throw new IllegalArgumentException("Service region must not exceed 100 characters");
		}
		if (accountNotes != null && accountNotes.length() > 2000) {
			throw new IllegalArgumentException("Account notes must not exceed 2000 characters");
		}
		serviceRegion = serviceRegion.trim();
	}
}
