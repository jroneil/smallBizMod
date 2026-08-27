package com.oneilsoftware.sbp.module.job.api;

import java.util.UUID;

/**
 * Thrown when a Job operation targets a Job id that does not exist. Mapped to 404 by the web layer.
 */
public class UnknownJobException extends RuntimeException {

	public UnknownJobException(UUID jobId) {
		super("Job does not exist: " + jobId);
	}
}
