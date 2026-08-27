package com.oneilsoftware.sbp.module.document.api;

/**
 * Command to replace a Document's content in place — same Document identity and metadata row
 * (DOC-FR-05, ADR-DOC-003). Replacement does not create version history in v0.1: only the latest
 * content is retrievable.
 */
public record ReplaceDocumentContent(String contentType, byte[] content) {

	public ReplaceDocumentContent {
		contentType = CreateDocument.normalizeContentType(contentType);
		if (content == null || content.length == 0) {
			throw new IllegalArgumentException("Document content is required");
		}
	}
}
