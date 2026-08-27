package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.time.Instant;

public record AuditFilter(
		String actor,
		String subjectType,
		String subjectId,
		String action,
		Instant from,
		Instant to,
		Integer limit) {
}
