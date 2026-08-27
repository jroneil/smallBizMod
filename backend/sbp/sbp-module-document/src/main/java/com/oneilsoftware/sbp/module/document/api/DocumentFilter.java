package com.oneilsoftware.sbp.module.document.api;

import java.util.UUID;

/**
 * Search/filter for the paginated Document list (DOC-FR-02). {@code query} is matched
 * case-insensitively against the display name; {@code ownerType} plus {@code ownerId} narrow the
 * list to one Customer's or one Job's Documents (listing by owner is a filter of this single list
 * contract, not a separate endpoint). Pagination is part of the baseline contract from the start:
 * {@code page} is zero-based and {@code size} defaults to 20 and is capped at 100.
 */
public record DocumentFilter(
		String query,
		DocumentOwnerType ownerType,
		UUID ownerId,
		int page,
		int size) {

	public static final int DEFAULT_PAGE_SIZE = 20;
	public static final int MAX_PAGE_SIZE = 100;

	public DocumentFilter {
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
