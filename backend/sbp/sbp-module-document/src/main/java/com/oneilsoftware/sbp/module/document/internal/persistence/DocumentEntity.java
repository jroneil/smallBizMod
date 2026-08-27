package com.oneilsoftware.sbp.module.document.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Metadata row of a Document (ADR-DOC-001/003). Exactly one of {@code customerId}/{@code jobId}
 * is set (enforced by the {@code ck_sbp_document_exactly_one_owner} CHECK constraint); content
 * never lives in this table — it is reached only through the storage abstraction.
 */
@Entity
@Table(name = "sbp_document")
public class DocumentEntity {

	@Id
	private UUID id;

	@Column(name = "customer_id")
	private UUID customerId;

	@Column(name = "job_id")
	private UUID jobId;

	@Column(name = "display_name", nullable = false, length = 255)
	private String displayName;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected DocumentEntity() {
	}

	public DocumentEntity(
			UUID id,
			UUID customerId,
			UUID jobId,
			String displayName,
			String contentType,
			long sizeBytes,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.customerId = customerId;
		this.jobId = jobId;
		this.displayName = displayName;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void rename(String displayName, Instant updatedAt) {
		this.displayName = displayName;
		this.updatedAt = updatedAt;
	}

	public void replaceContent(String contentType, long sizeBytes, Instant updatedAt) {
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public UUID getJobId() {
		return jobId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getContentType() {
		return contentType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
