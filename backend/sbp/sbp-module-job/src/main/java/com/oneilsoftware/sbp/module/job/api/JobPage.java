package com.oneilsoftware.sbp.module.job.api;

import java.util.List;

/**
 * Paginated Job list result (JOB-FR-02). {@code page} is the zero-based page number of this result,
 * {@code size} the requested page size, {@code totalElements} the total number of matching Jobs,
 * and {@code totalPages} the total number of pages. Items are ordered most recently created first.
 */
public record JobPage(
		List<Job> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public JobPage {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
