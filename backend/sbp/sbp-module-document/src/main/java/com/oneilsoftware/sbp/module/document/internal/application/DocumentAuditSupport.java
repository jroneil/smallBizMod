package com.oneilsoftware.sbp.module.document.internal.application;

import java.util.List;

import com.oneilsoftware.sbp.framework.audit.AuditActorResolver;
import com.oneilsoftware.sbp.framework.audit.AuditEvent;

/**
 * Small internal helper that records framework audit events for Document mutations with the
 * current actor and timestamp. It keeps the business service free of framework-audit plumbing
 * while recording only changed field names — never old/new values, never file content or content
 * hashes.
 */
public final class DocumentAuditSupport {

	private final DocumentAuditRecorder recorder;
	private final AuditActorResolver actorResolver;

	public DocumentAuditSupport(DocumentAuditRecorder recorder, AuditActorResolver actorResolver) {
		this.recorder = recorder;
		this.actorResolver = actorResolver;
	}

	public void record(String subjectType, String subjectId, String action, List<String> changedFields, String detail) {
		recorder.record(new AuditEvent(
				actorResolver.currentActor(),
				subjectType,
				subjectId,
				action,
				changedFields,
				detail,
				Timestamps.now()));
	}
}
