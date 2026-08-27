package com.oneilsoftware.sbp.module.document.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Document-owned audit persistence (DOC-FR-09, ADR-DOC-004). Events are written through the
 * framework audit hook in the same transaction as the business change. Records are value-free:
 * changed field names and small markers only, never old/new values, never file content or content
 * hashes. Rows survive Document deletion so history remains readable (DOC-FR-06).
 */
@Entity
@Table(name = "sbp_document_audit_event")
public class DocumentAuditEventEntity {

	@Id
	private UUID id;

	@Column(length = 100)
	private String actor;

	@Column(name = "subject_type", nullable = false, length = 20)
	private String subjectType;

	@Column(name = "subject_id", nullable = false, length = 64)
	private String subjectId;

	@Column(nullable = false, length = 40)
	private String action;

	@Column(name = "changed_fields", length = 500)
	private String changedFields;

	@Column(length = 200)
	private String detail;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected DocumentAuditEventEntity() {
	}

	public DocumentAuditEventEntity(
			UUID id,
			String actor,
			String subjectType,
			String subjectId,
			String action,
			String changedFields,
			String detail,
			Instant occurredAt) {
		this.id = id;
		this.actor = actor;
		this.subjectType = subjectType;
		this.subjectId = subjectId;
		this.action = action;
		this.changedFields = changedFields;
		this.detail = detail;
		this.occurredAt = occurredAt;
	}

	public UUID getId() {
		return id;
	}

	public String getActor() {
		return actor;
	}

	public String getSubjectType() {
		return subjectType;
	}

	public String getSubjectId() {
		return subjectId;
	}

	public String getAction() {
		return action;
	}

	public String getChangedFields() {
		return changedFields;
	}

	public String getDetail() {
		return detail;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}
}
