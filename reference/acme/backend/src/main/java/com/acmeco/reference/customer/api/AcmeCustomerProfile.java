package com.acmeco.reference.customer.api;

import java.util.UUID;

public record AcmeCustomerProfile(
		UUID customerId,
		String serviceRegion,
		String accountNotes) {
}
