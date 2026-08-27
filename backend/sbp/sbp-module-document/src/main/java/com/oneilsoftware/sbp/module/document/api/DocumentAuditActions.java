package com.oneilsoftware.sbp.module.document.api;

/**
 * Well-known audit subject types and actions recorded by the Document module (DOC-FR-09,
 * ADR-DOC-004). Kept as plain strings so customer extensions can add their own actions without
 * changing the module. Records carry field-name markers only — never old/new values, never file
 * content or content hashes.
 */
public final class DocumentAuditActions {

	public static final String SUBJECT_DOCUMENT = "DOCUMENT";

	public static final String DOCUMENT_CREATED = "DOCUMENT_CREATED";
	public static final String DOCUMENT_METADATA_UPDATED = "DOCUMENT_METADATA_UPDATED";
	public static final String DOCUMENT_CONTENT_REPLACED = "DOCUMENT_CONTENT_REPLACED";
	public static final String DOCUMENT_DELETED = "DOCUMENT_DELETED";

	private DocumentAuditActions() {
	}
}
