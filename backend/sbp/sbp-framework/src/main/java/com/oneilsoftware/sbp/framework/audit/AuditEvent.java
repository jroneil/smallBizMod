package com.oneilsoftware.sbp.framework.audit;

import java.time.Instant;
import java.util.List;

/**
 * Minimal, framework-neutral audit hook payload (framework FR-10).
 *
 * <p>{@code changedFields} is a list of field <em>names</em> that changed — never old/new values.
 * Personal-data values must not be placed in this record; encrypted fields are represented by a
 * changed-field marker (or a hash) rather than plaintext.
 */
public record AuditEvent(
		String actor,
		String subjectType,
		String subjectId,
		String action,
		List<String> changedFields,
		String detail,
		Instant occurredAt) {

	public AuditEvent {
		changedFields = changedFields == null ? List.of() : List.copyOf(changedFields);
	}
}
