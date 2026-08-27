package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.time.Instant;
import java.util.UUID;

public record Group(
		UUID id,
		String name,
		String description,
		GroupStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
