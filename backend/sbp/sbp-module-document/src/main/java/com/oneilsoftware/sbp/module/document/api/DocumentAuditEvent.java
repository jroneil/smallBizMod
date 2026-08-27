package com.oneilsoftware.sbp.module.document.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A persisted, read-only audit event for one Document (DOC-FR-09/10, ADR-DOC-004). Field values
 * are never stored — only the names of changed fields — and file content and content hashes are
 * never stored. The subject is always the Document identified by the request path, so subject
 * type/id are omitted from this per-Document read model. Events remain readable after the Document
 * itself is deleted (DOC-FR-06).
 */
public record DocumentAuditEvent(
		UUID id,
		String actor,
		String action,
		List<String> changedFields,
		String detail,
		Instant occurredAt) {

	public DocumentAuditEvent {
		changedFields = changedFields == null ? List.of() : List.copyOf(changedFields);
	}
}
