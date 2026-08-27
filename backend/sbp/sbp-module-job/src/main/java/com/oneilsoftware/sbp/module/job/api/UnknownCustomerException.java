package com.oneilsoftware.sbp.module.job.api;

import java.util.UUID;

/**
 * Thrown when a Job is created for a Customer that does not exist. The Customer identifier is
 * validated through the public Customer contract, never Customer persistence internals
 * (JOB-FR-06). Mapped to 404 by the web layer.
 */
public class UnknownCustomerException extends RuntimeException {

	public UnknownCustomerException(UUID customerId) {
		super("Customer does not exist: " + customerId);
	}
}
