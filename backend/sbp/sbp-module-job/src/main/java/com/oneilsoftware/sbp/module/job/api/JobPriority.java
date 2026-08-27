package com.oneilsoftware.sbp.module.job.api;

/**
 * Deliberately small, closed priority triage set (JOB-FR-07). Display and filter semantics only:
 * no SLA calculation, escalation rules, priority queues, or configurable priority sets.
 */
public enum JobPriority {
	LOW,
	NORMAL,
	HIGH
}
