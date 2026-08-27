package com.oneilsoftware.sbp.module.document.api;

import java.util.List;

/**
 * Paginated Document list result (DOC-FR-02). {@code page} is the zero-based page number of this
 * result, {@code size} the requested page size, {@code totalElements} the total number of matching
 * Documents, and {@code totalPages} the total number of pages. Items are ordered most recently
 * created first.
 */
public record DocumentPage(
		List<Document> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public DocumentPage {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
