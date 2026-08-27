package com.oneilsoftware.sbp.module.job.api;

import java.util.UUID;

/**
 * Search/filter for the paginated Job list (JOB-FR-02). {@code query} is matched case-insensitively
 * against title and description; {@code customerId}, {@code status}, and {@code priority} narrow the
 * list. Pagination is part of the baseline contract from the start: {@code page} is zero-based and
 * {@code size} defaults to 20 and is capped at 100.
 */
public record JobFilter(
		String query,
		UUID customerId,
		JobStatus status,
		JobPriority priority,
		int page,
		int size) {

	public static final int DEFAULT_PAGE_SIZE = 20;
	public static final int MAX_PAGE_SIZE = 100;

	public JobFilter {
		query = trimToNull(query);
		if (page < 0) {
			page = 0;
		}
		if (size < 1) {
			size = DEFAULT_PAGE_SIZE;
		}
		if (size > MAX_PAGE_SIZE) {
			size = MAX_PAGE_SIZE;
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
