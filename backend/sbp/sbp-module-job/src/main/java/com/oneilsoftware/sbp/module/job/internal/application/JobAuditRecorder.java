package com.oneilsoftware.sbp.module.job.internal.application;

import java.util.UUID;

import com.oneilsoftware.sbp.framework.audit.AuditEvent;
import com.oneilsoftware.sbp.framework.audit.AuditRecorder;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobAuditEventEntity;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobAuditEventRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job's implementation of the framework audit hook (JOB-FR-08, ADR-JOB-003). It persists events to
 * the Job-owned audit table. The audit save joins the caller's transaction, so the audit record is
 * committed atomically with the business change it describes.
 *
 * <p>The bean is intentionally exposed under this concrete type rather than as a generic
 * {@code AuditRecorder} bean: User Maintenance also provides an {@code AuditRecorder} implementation,
 * and two type-level beans would make {@code AuditRecorder} injection ambiguous in a composition that
 * includes both modules. The framework contract is still honored — this class implements
 * {@code AuditRecorder}.
 */
public class JobAuditRecorder implements AuditRecorder {

	private final JobAuditEventRepository repository;

	public JobAuditRecorder(JobAuditEventRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void record(AuditEvent event) {
		repository.save(new JobAuditEventEntity(
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
