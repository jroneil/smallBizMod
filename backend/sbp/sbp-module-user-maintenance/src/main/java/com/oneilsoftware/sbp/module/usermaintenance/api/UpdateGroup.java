package com.oneilsoftware.sbp.module.usermaintenance.api;

public record UpdateGroup(String name, String description) {

	public UpdateGroup {
		name = requireText("Group name", name, 100);
		description = trimToNull("Description", description, 500);
	}

	private static String requireText(String label, String value, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(label + " is required");
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}

	private static String trimToNull(String label, String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}
}
