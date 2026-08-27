package com.oneilsoftware.sbp.module.job.api;

/**
 * Thrown when a lifecycle action is not allowed from the Job's current state (JOB-FR-05). Only
 * OPEN → IN_PROGRESS, OPEN → CANCELLED, IN_PROGRESS → COMPLETED, and IN_PROGRESS → CANCELLED are
 * permitted; COMPLETED and CANCELLED are terminal. Mapped to 409 Conflict by the web layer.
 */
public class InvalidJobTransitionException extends RuntimeException {

	private final JobStatus from;
	private final JobStatus to;

	public InvalidJobTransitionException(JobStatus from, JobStatus to) {
		super("Cannot transition Job from " + from + " to " + to);
		this.from = from;
		this.to = to;
	}

	public JobStatus from() {
		return from;
	}

	public JobStatus to() {
		return to;
	}
}
