package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import java.util.List;

import com.oneilsoftware.sbp.framework.audit.AuditActorResolver;
import com.oneilsoftware.sbp.framework.audit.AuditEvent;
import com.oneilsoftware.sbp.framework.audit.AuditRecorder;

/**
 * Small internal helper that records framework audit events with the current actor and timestamp.
 * It keeps the business services free of framework-audit plumbing while never exposing plaintext
 * personal-data values (only changed field names are recorded).
 */
public final class AuditSupport {

	private final AuditRecorder recorder;
	private final AuditActorResolver actorResolver;

	public AuditSupport(AuditRecorder recorder, AuditActorResolver actorResolver) {
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
