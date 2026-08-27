package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sbp_user_audit_event")
public class UserAuditEventEntity {

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

	protected UserAuditEventEntity() {
	}

	public UserAuditEventEntity(
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
