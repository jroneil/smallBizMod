package com.oneilsoftware.sbp.module.job.api;

/**
 * Well-known audit subject types and actions recorded by the Job module (JOB-FR-08). Kept as plain
 * strings so customer extensions can add their own actions without changing the module.
 * {@code JOB_ASSIGNED} is reserved for a future assignment capability (ADR-JOB-002) and is not
 * recorded in v0.1.
 */
public final class JobAuditActions {

	public static final String SUBJECT_JOB = "JOB";

	public static final String JOB_CREATED = "JOB_CREATED";
	public static final String JOB_UPDATED = "JOB_UPDATED";
	public static final String JOB_STATUS_CHANGED = "JOB_STATUS_CHANGED";

	private JobAuditActions() {
	}
}
