package com.oneilsoftware.sbp.module.usermaintenance.api;

/**
 * Edits the permitted profile fields of an existing User. The username is the stable
 * application identity and is not editable through this command; lifecycle status changes use
 * the dedicated activate/deactivate operations rather than a generic field update.
 */
public record UpdateUser(
		String email,
		String givenName,
		String familyName,
		String phone) {

	public UpdateUser {
		email = requireText("Email", email, 320);
		givenName = requireText("Given name", givenName, 100);
		familyName = requireText("Family name", familyName, 100);
		phone = trimToNull("Phone", phone, 40);
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
