package com.oneilsoftware.sbp.module.document.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public contract of the reusable Document capability (ADR-DOC-001). Document validates owner
 * references (Customer or Job) only through the owning modules' public contracts, stores the
 * stable owner UUIDs, creates no cross-module database foreign key, and stores content only
 * through the framework storage abstraction (DOC-FR-08, ADR-DOC-002).
 */
public interface DocumentOperations {

	/**
	 * Uploads a Document for exactly one owner. Unknown owners are rejected with
	 * {@link UnknownDocumentOwnerException}; content over the configured size limit is rejected
	 * with {@link DocumentSizeLimitExceededException} before anything is stored.
	 */
	Document create(CreateDocument command);

	/**
	 * Paginated, searchable/filterable Document list (DOC-FR-02), most recently created first.
	 */
	DocumentPage search(DocumentFilter filter);

	Optional<Document> findById(UUID documentId);

	/**
	 * Renames a Document's display name (DOC-FR-03), audited as {@code DOCUMENT_METADATA_UPDATED}.
	 */
	Document updateDisplayName(UUID documentId, UpdateDocumentMetadata command);

	/**
	 * Replaces a Document's content in place — same identity and metadata row (DOC-FR-05),
	 * audited as {@code DOCUMENT_CONTENT_REPLACED}. No version history is created in v0.1.
	 */
	Document replaceContent(UUID documentId, ReplaceDocumentContent command);

	/**
	 * Deletes a Document: removes the metadata record and its stored content through the storage
	 * abstraction (DOC-FR-06), audited as {@code DOCUMENT_DELETED}. Never cascades to the owner
	 * Customer/Job record. The Document's audit history remains readable afterward.
	 */
	void delete(UUID documentId);

	/**
	 * Downloads a Document's content with its stored advisory content type and display name
	 * (DOC-FR-04).
	 */
	DocumentContent download(UUID documentId);

	/**
	 * Read-only audit events for one Document (DOC-FR-10, ADR-DOC-004), newest first.
	 * {@code action} optionally narrows to one event type (e.g., DOCUMENT_DELETED). Unlike the
	 * other operations this does not require the Document to still exist: audit history remains
	 * readable after deletion (DOC-FR-06).
	 */
	List<DocumentAuditEvent> listAuditEvents(UUID documentId, String action);
}
