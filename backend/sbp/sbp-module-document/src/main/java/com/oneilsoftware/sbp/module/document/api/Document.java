package com.oneilsoftware.sbp.module.document.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Reusable Document record: a stored file (content) plus descriptive metadata, attached to exactly
 * one owner — a Customer or a Job (DOC-FR-07, ADR-DOC-001/003).
 *
 * <p>Metadata never includes content bytes; content is retrieved only through
 * {@link DocumentOperations#download}. {@code contentType} is advisory metadata for display and
 * download headers, not an authorization decision (NFR-DOC-07).
 */
public record Document(
		UUID id,
		DocumentOwnerType ownerType,
		UUID ownerId,
		String displayName,
		String contentType,
		long sizeBytes,
		Instant createdAt,
		Instant updatedAt) {
}
