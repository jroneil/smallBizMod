package com.oneilsoftware.sbp.module.contact.api;

import java.util.UUID;

public record Contact(
		UUID id,
		UUID customerId,
		String fullName,
		String email,
		String phone,
		String roleTitle,
		ContactStatus status) {
}
