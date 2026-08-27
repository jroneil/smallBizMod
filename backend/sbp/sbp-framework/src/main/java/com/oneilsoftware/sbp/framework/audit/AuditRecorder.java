package com.oneilsoftware.sbp.framework.audit;

/**
 * Framework audit hook (framework FR-10): records a single, immutable audit event. Business modules
 * and customer extensions call this after a relevant action; the framework and customer decide how
 * events are persisted and rendered.
 */
@FunctionalInterface
public interface AuditRecorder {

	void record(AuditEvent event);
}
