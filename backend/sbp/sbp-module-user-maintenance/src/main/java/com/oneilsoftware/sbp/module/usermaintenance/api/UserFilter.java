package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.UUID;

/**
 * Search/filter for the User list. {@code query} is matched against username (substring) and the
 * normalized name/email blind-index tokens (exact). {@code status}, {@code roleId}, and
 * {@code groupId} narrow the list by lifecycle state and assignments.
 */
public record UserFilter(
		String query,
		UserStatus status,
		UUID roleId,
		UUID groupId) {

	public UserFilter {
		query = trimToNull(query);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
