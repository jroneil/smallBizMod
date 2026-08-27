package com.oneilsoftware.sbp.module.job.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Reusable Job record: work performed for or on behalf of a Customer (ADR-JOB-001/002).
 *
 * <p>{@code scheduledDate} is the single scheduling touchpoint on the Job in v0.1: the date the
 * work is currently planned to start. It carries no recurrence, time-slot, or resource semantics.
 */
public record Job(
		UUID id,
		UUID customerId,
		String title,
		String description,
		JobStatus status,
		JobPriority priority,
		LocalDate scheduledDate,
		Instant createdAt,
		Instant updatedAt) {
}
