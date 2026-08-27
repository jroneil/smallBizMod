package com.oneilsoftware.sbp.module.job.api;

/**
 * Lifecycle state of a Job. Deliberately small: OPEN, IN_PROGRESS, COMPLETED (terminal), and
 * CANCELLED (terminal). Transitions are enforced by {@link JobOperations}; there is no hard delete
 * (framework FR-21).
 */
public enum JobStatus {
	OPEN,
	IN_PROGRESS,
	COMPLETED,
	CANCELLED
}
