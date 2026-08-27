package com.oneilsoftware.sbp.module.job.api;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to create a Job for an existing Customer. The Customer identifier is validated through
 * the public Customer contract by {@link JobOperations}; the Job module never reads Customer
 * persistence internals (JOB-FR-06, ADR-JOB-001).
 */
public record CreateJob(
		UUID customerId,
		String title,
		String description,
		JobPriority priority,
		LocalDate scheduledDate) {

	public CreateJob {
		if (customerId == null) {
			throw new IllegalArgumentException("Customer id is required");
		}
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("Job title is required");
		}
		title = title.trim();
		description = trimToNull(description);
		priority = priority == null ? JobPriority.NORMAL : priority;

		if (title.length() > 200) {
			throw new IllegalArgumentException("Job title must not exceed 200 characters");
		}
		if (description != null && description.length() > 2000) {
			throw new IllegalArgumentException("Job description must not exceed 2000 characters");
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
