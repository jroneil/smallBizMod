package com.oneilsoftware.sbp.module.usermaintenance.api;

/**
 * A raw, unvalidated User row for bulk import. Field validation and username-uniqueness checks
 * happen per row inside {@link UserOperations#bulkCreate}; invalid rows are reported as errors
 * rather than failing the whole batch.
 */
public record BulkCreateUser(
		String username,
		String email,
		String givenName,
		String familyName,
		String phone) {
}
