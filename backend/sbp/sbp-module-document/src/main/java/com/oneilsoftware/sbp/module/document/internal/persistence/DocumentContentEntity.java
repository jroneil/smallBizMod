package com.oneilsoftware.sbp.module.document.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Content row of the module's reference database-backed {@code BlobStorage} implementation
 * (ADR-DOC-002): the storage key (the Document UUID) maps to the stored bytes. This table is
 * reached only through the framework storage abstraction and is not part of the Document metadata
 * mapping.
 */
@Entity
@Table(name = "sbp_document_content")
public class DocumentContentEntity {

	@Id
	@Column(name = "storage_key")
	private UUID storageKey;

	@Column(nullable = false)
	private byte[] content;

	@Column(name = "stored_at", nullable = false)
	private Instant storedAt;

	protected DocumentContentEntity() {
	}

	public DocumentContentEntity(UUID storageKey, byte[] content, Instant storedAt) {
		this.storageKey = storageKey;
		this.content = content;
		this.storedAt = storedAt;
	}

	public UUID getStorageKey() {
		return storageKey;
	}

	public byte[] getContent() {
		return content;
	}

	public Instant getStoredAt() {
		return storedAt;
	}
}
