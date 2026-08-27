package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

import java.time.Instant;
import java.util.List;

import com.oneilsoftware.sbp.module.usermaintenance.api.AuditFilter;
import com.oneilsoftware.sbp.module.usermaintenance.api.AuditOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserAuditEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only audit viewer (UM-FR-17). Filters by actor, subject type/id, event action, and a date
 * range; results are returned newest-first and capped by an optional limit.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

	private final AuditOperations auditOperations;

	public AuditController(AuditOperations auditOperations) {
		this.auditOperations = auditOperations;
	}

	@GetMapping
	public List<UserAuditEvent> search(
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) String subjectType,
			@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String action,
			@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to,
			@RequestParam(required = false) Integer limit) {
		return auditOperations.search(new AuditFilter(
				actor, subjectType, subjectId, action, from, to, limit));
	}
}
