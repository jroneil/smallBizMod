package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

/**
 * Raw bulk-import row. No bean validation is applied here so per-row validation can be performed
 * and reported by the application service instead of failing the whole request.
 */
record BulkCreateUserRequest(
		String username,
		String email,
		String givenName,
		String familyName,
		String phone) {
}
