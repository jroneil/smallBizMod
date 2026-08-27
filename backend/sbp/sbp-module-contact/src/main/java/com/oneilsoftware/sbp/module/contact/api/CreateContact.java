package com.oneilsoftware.sbp.module.contact.api;

import java.util.UUID;

public record CreateContact(
		UUID customerId,
		String fullName,
		String email,
		String phone,
		String roleTitle) {

	public CreateContact {
		if (customerId == null) {
			throw new IllegalArgumentException("Customer id is required");
		}
		if (fullName == null || fullName.isBlank()) {
			throw new IllegalArgumentException("Contact name is required");
		}

		fullName = fullName.trim();
		email = trimToNull(email);
		phone = trimToNull(phone);
		roleTitle = trimToNull(roleTitle);

		if (fullName.length() > 200) {
			throw new IllegalArgumentException("Contact name must not exceed 200 characters");
		}
		if (email != null && email.length() > 200) {
			throw new IllegalArgumentException("Email must not exceed 200 characters");
		}
		if (phone != null && phone.length() > 40) {
			throw new IllegalArgumentException("Phone must not exceed 40 characters");
		}
		if (roleTitle != null && roleTitle.length() > 100) {
			throw new IllegalArgumentException("Role/title must not exceed 100 characters");
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
