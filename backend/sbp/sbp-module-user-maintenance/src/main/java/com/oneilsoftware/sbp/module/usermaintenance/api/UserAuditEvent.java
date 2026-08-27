package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A persisted, read-only audit event owned by User Maintenance. Field values are not stored — only
 * the names of changed fields — so personal-data plaintext never appears in audit records.
 */
public record UserAuditEvent(
		UUID id,
		String actor,
		String subjectType,
		String subjectId,
		String action,
		List<String> changedFields,
		String detail,
		Instant occurredAt) {
}
