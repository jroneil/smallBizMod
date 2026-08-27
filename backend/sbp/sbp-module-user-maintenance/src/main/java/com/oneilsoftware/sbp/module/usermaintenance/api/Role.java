package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.time.Instant;
import java.util.UUID;

public record Role(
		UUID id,
		String name,
		String description,
		RoleStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
