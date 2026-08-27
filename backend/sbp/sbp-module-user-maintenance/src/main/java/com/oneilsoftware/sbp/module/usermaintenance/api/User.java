package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Reusable application/business identity and user-maintenance record.
 *
 * <p>This is distinct from customer-owned authentication identity: the SBP User is a
 * maintenance record, while authentication mechanism, identity provider, and authorization
 * policy remain customer-project decisions.
 */
public record User(
		UUID id,
		String username,
		String email,
		String givenName,
		String familyName,
		String phone,
		UserStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
