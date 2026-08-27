package com.oneilsoftware.sbp.module.job.api;

import java.time.LocalDate;

/**
 * Command to replace the editable (non-status) fields of a Job: title, description, priority, and
 * scheduled date. Full-replace semantics: a null {@code scheduledDate} clears the date, and a null
 * {@code priority} means NORMAL. Status is changed only through the lifecycle actions
 * ({@link JobOperations#start}, {@link JobOperations#complete}, {@link JobOperations#cancel}).
 */
public record UpdateJob(
		String title,
		String description,
		JobPriority priority,
		LocalDate scheduledDate) {

	public UpdateJob {
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
