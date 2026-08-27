package com.oneilsoftware.sbp.module.job.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public contract of the reusable Job capability (ADR-JOB-001). Job validates Customer references
 * only through the owning module's public contract, stores the stable Customer UUID, and creates no
 * cross-module database foreign key.
 */
public interface JobOperations {

	Job create(CreateJob command);

	JobPage search(JobFilter filter);

	Optional<Job> findById(UUID jobId);

	Job update(UUID jobId, UpdateJob command);

	Job start(UUID jobId);

	Job complete(UUID jobId);

	Job cancel(UUID jobId);

	List<Job> listByCustomer(UUID customerId);

	/**
	 * Read-only audit events for one Job (JOB-FR-09), newest first. {@code action} optionally
	 * narrows to one event type (e.g., JOB_STATUS_CHANGED). Unknown jobs are rejected with
	 * {@link UnknownJobException}.
	 */
	List<JobAuditEvent> listAuditEvents(UUID jobId, String action);
}
