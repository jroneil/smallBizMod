package com.oneilsoftware.sbp.module.document.api;

import java.util.UUID;

/**
 * Command to upload a Document for exactly one owner — a Customer or a Job (DOC-FR-01,
 * ADR-DOC-001). The owner identifier is validated through the public Customer/Job contracts by
 * {@link DocumentOperations}; the Document module never reads Customer or Job persistence
 * internals.
 *
 * <p>The display name is the human-meaningful file name; any path components are stripped so only
 * the base name is stored and served. The upload size limit is enforced by the application service
 * ({@link DocumentSizeLimitExceededException}), not by this command.
 */
public record CreateDocument(
		DocumentOwnerType ownerType,
		UUID ownerId,
		String displayName,
		String contentType,
		byte[] content) {

	public CreateDocument {
		if (ownerType == null) {
			throw new IllegalArgumentException("Document owner type is required");
		}
		if (ownerId == null) {
			throw new IllegalArgumentException("Document owner id is required");
		}
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("Document display name is required");
		}
		displayName = baseName(displayName.trim());
		if (displayName.length() > 255) {
			throw new IllegalArgumentException("Document display name must not exceed 255 characters");
		}
		contentType = normalizeContentType(contentType);
		if (content == null || content.length == 0) {
			throw new IllegalArgumentException("Document content is required");
		}
	}

	static String normalizeContentType(String value) {
		if (value == null || value.isBlank()) {
			return "application/octet-stream";
		}
		String trimmed = value.trim();
		return trimmed.length() > 100 ? "application/octet-stream" : trimmed;
	}

	/**
	 * Strips any path components so only the base file name is stored (never a path).
	 */
	static String baseName(String name) {
		int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		return slash >= 0 ? name.substring(slash + 1) : name;
	}
}
