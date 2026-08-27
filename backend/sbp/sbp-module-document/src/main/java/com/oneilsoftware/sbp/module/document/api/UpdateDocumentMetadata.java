package com.oneilsoftware.sbp.module.document.api;

/**
 * Command to rename a Document's display name (DOC-FR-03). Only the display name is editable
 * metadata in v0.1; all other fields are managed by their own operations.
 */
public record UpdateDocumentMetadata(String displayName) {

	public UpdateDocumentMetadata {
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("Document display name is required");
		}
		displayName = CreateDocument.baseName(displayName.trim());
		if (displayName.length() > 255) {
			throw new IllegalArgumentException("Document display name must not exceed 255 characters");
		}
	}
}
