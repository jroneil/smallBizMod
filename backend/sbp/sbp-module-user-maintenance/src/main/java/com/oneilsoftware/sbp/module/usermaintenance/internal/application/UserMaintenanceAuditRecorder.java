package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import java.util.UUID;

import com.oneilsoftware.sbp.framework.audit.AuditEvent;
import com.oneilsoftware.sbp.framework.audit.AuditRecorder;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserAuditEventEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserAuditEventRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Maintenance's implementation of the framework audit hook. It persists audit events to the
 * module-owned audit table. The audit save joins the caller's transaction, so the audit record is
 * committed atomically with the business change it describes.
 */
public class UserMaintenanceAuditRecorder implements AuditRecorder {

	private final UserAuditEventRepository repository;

	public UserMaintenanceAuditRecorder(UserAuditEventRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void record(AuditEvent event) {
		repository.save(new UserAuditEventEntity(
				UUID.randomUUID(),
				event.actor(),
				event.subjectType(),
				event.subjectId(),
				event.action(),
				event.changedFields().isEmpty() ? null : String.join(",", event.changedFields()),
				event.detail(),
				event.occurredAt() != null ? event.occurredAt() : Timestamps.now()));
	}
}
