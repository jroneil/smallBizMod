package com.oneilsoftware.sbp.module.job.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A persisted, read-only audit event for one Job (JOB-FR-09, ADR-JOB-003). Field values are never
 * stored — only the names of changed fields — and status transitions carry a marker in
 * {@code detail} (e.g., "OPEN→IN_PROGRESS"). The subject is always the Job identified by the
 * request path, so subject type/id are omitted from this per-Job read model.
 */
public record JobAuditEvent(
		UUID id,
		String actor,
		String action,
		List<String> changedFields,
		String detail,
		Instant occurredAt) {

	public JobAuditEvent {
		changedFields = changedFields == null ? List.of() : List.copyOf(changedFields);
	}
}
