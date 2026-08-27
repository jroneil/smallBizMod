package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.List;

public record BulkUserImportResult(List<User> created, List<BulkUserImportError> errors) {

	public BulkUserImportResult {
		created = created == null ? List.of() : List.copyOf(created);
		errors = errors == null ? List.of() : List.copyOf(errors);
	}
}
