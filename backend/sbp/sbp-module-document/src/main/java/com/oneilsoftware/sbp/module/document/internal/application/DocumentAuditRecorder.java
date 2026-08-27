package com.oneilsoftware.sbp.module.document.internal.application;

import java.util.UUID;

import com.oneilsoftware.sbp.framework.audit.AuditEvent;
import com.oneilsoftware.sbp.framework.audit.AuditRecorder;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentAuditEventEntity;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentAuditEventRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Document's implementation of the framework audit hook (DOC-FR-09, ADR-DOC-004). It persists
 * events to the Document-owned audit table. The audit save joins the caller's transaction, so the
 * audit record is committed atomically with the business change it describes.
 *
 * <p>The bean is intentionally exposed under this concrete type rather than as a generic
 * {@code AuditRecorder} bean: User Maintenance and Job also provide {@code AuditRecorder}
 * implementations, and multiple type-level beans would make {@code AuditRecorder} injection
 * ambiguous in a composition that includes several modules. The framework contract is still
 * honored — this class implements {@code AuditRecorder}.
 */
public class DocumentAuditRecorder implements AuditRecorder {

	private final DocumentAuditEventRepository repository;

	public DocumentAuditRecorder(DocumentAuditEventRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void record(AuditEvent event) {
		repository.save(new DocumentAuditEventEntity(
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
