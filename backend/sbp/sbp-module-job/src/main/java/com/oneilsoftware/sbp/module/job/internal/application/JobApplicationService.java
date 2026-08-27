package com.oneilsoftware.sbp.module.job.internal.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.job.api.CreateJob;
import com.oneilsoftware.sbp.module.job.api.InvalidJobTransitionException;
import com.oneilsoftware.sbp.module.job.api.Job;
import com.oneilsoftware.sbp.module.job.api.JobAuditActions;
import com.oneilsoftware.sbp.module.job.api.JobAuditEvent;
import com.oneilsoftware.sbp.module.job.api.JobFilter;
import com.oneilsoftware.sbp.module.job.api.JobOperations;
import com.oneilsoftware.sbp.module.job.api.JobPage;
import com.oneilsoftware.sbp.module.job.api.JobPriority;
import com.oneilsoftware.sbp.module.job.api.JobStatus;
import com.oneilsoftware.sbp.module.job.api.UnknownCustomerException;
import com.oneilsoftware.sbp.module.job.api.UnknownJobException;
import com.oneilsoftware.sbp.module.job.api.UpdateJob;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobAuditEventEntity;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobAuditEventRepository;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobEntity;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing the Job public contract. Customer references are validated only
 * through {@link CustomerOperations}; the Job module never touches Customer persistence internals
 * (JOB-FR-06, ADR-JOB-001). Every create, edit, and status change is audited in the same
 * transaction through the framework audit hook (JOB-FR-08, ADR-JOB-003).
 */
public class JobApplicationService implements JobOperations {

	private final JobRepository jobRepository;
	private final CustomerOperations customerOperations;
	private final JobAuditEventRepository jobAuditEventRepository;
	private final JobAuditSupport auditSupport;

	public JobApplicationService(
			JobRepository jobRepository,
			CustomerOperations customerOperations,
			JobAuditEventRepository jobAuditEventRepository,
			JobAuditSupport auditSupport) {
		this.jobRepository = jobRepository;
		this.customerOperations = customerOperations;
		this.jobAuditEventRepository = jobAuditEventRepository;
		this.auditSupport = auditSupport;
	}

	@Override
	@Transactional
	public Job create(CreateJob command) {
		if (customerOperations.findById(command.customerId()).isEmpty()) {
			throw new UnknownCustomerException(command.customerId());
		}

		Instant now = Timestamps.now();
		JobEntity job = new JobEntity(
				UUID.randomUUID(),
				command.customerId(),
				command.title(),
				command.description(),
				JobStatus.OPEN,
				command.priority(),
				command.scheduledDate(),
				now,
				now);
		Job saved = toJob(jobRepository.save(job));
		auditSupport.record(
				JobAuditActions.SUBJECT_JOB,
				saved.id().toString(),
				JobAuditActions.JOB_CREATED,
				List.of("customerId", "title", "description", "status", "priority", "scheduledDate"),
				null);
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public JobPage search(JobFilter filter) {
		Page<JobEntity> page = jobRepository.search(
				filter.query(),
				filter.customerId(),
				filter.status(),
				filter.priority(),
				PageRequest.of(filter.page(), filter.size()));
		return new JobPage(
				page.getContent().stream().map(JobApplicationService::toJob).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Job> findById(UUID jobId) {
		return jobRepository.findById(jobId).map(JobApplicationService::toJob);
	}

	@Override
	@Transactional
	public Job update(UUID jobId, UpdateJob command) {
		JobEntity job = requireJob(jobId);
		List<String> changed = new ArrayList<>();
		if (!Objects.equals(job.getTitle(), command.title())) {
			changed.add("title");
		}
		if (!Objects.equals(job.getDescription(), command.description())) {
			changed.add("description");
		}
		if (!Objects.equals(job.getPriority(), command.priority())) {
			changed.add("priority");
		}
		if (!Objects.equals(job.getScheduledDate(), command.scheduledDate())) {
			changed.add("scheduledDate");
		}
		job.updateFields(
				command.title(),
				command.description(),
				command.priority(),
				command.scheduledDate(),
				Timestamps.now());
		Job saved = toJob(jobRepository.save(job));
		auditSupport.record(
				JobAuditActions.SUBJECT_JOB,
				saved.id().toString(),
				JobAuditActions.JOB_UPDATED,
				changed,
				null);
		return saved;
	}

	@Override
	@Transactional
	public Job start(UUID jobId) {
		return changeStatus(jobId, JobStatus.IN_PROGRESS);
	}

	@Override
	@Transactional
	public Job complete(UUID jobId) {
		return changeStatus(jobId, JobStatus.COMPLETED);
	}

	@Override
	@Transactional
	public Job cancel(UUID jobId) {
		return changeStatus(jobId, JobStatus.CANCELLED);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Job> listByCustomer(UUID customerId) {
		return jobRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
				.map(JobApplicationService::toJob)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobAuditEvent> listAuditEvents(UUID jobId, String action) {
		requireJob(jobId);
		return jobAuditEventRepository.findBySubjectId(jobId.toString(), trimToNull(action)).stream()
				.map(JobApplicationService::toAuditEvent)
				.toList();
	}

	private Job changeStatus(UUID jobId, JobStatus target) {
		JobEntity job = requireJob(jobId);
		JobStatus from = job.getStatus();
		if (!isAllowedTransition(from, target)) {
			throw new InvalidJobTransitionException(from, target);
		}
		job.changeStatus(target, Timestamps.now());
		Job saved = toJob(jobRepository.save(job));
		auditSupport.record(
				JobAuditActions.SUBJECT_JOB,
				saved.id().toString(),
				JobAuditActions.JOB_STATUS_CHANGED,
				List.of("status"),
				from + "→" + target);
		return saved;
	}

	private static boolean isAllowedTransition(JobStatus from, JobStatus to) {
		return switch (from) {
			case OPEN -> to == JobStatus.IN_PROGRESS || to == JobStatus.CANCELLED;
			case IN_PROGRESS -> to == JobStatus.COMPLETED || to == JobStatus.CANCELLED;
			default -> false;
		};
	}

	private JobEntity requireJob(UUID jobId) {
		return jobRepository.findById(jobId)
				.orElseThrow(() -> new UnknownJobException(jobId));
	}

	private static JobAuditEvent toAuditEvent(JobAuditEventEntity entity) {
		return new JobAuditEvent(
				entity.getId(),
				entity.getActor(),
				entity.getAction(),
				split(entity.getChangedFields()),
				entity.getDetail(),
				entity.getOccurredAt());
	}

	private static List<String> split(String joined) {
		if (joined == null || joined.isBlank()) {
			return List.of();
		}
		return Arrays.asList(joined.split(","));
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static Job toJob(JobEntity entity) {
		return new Job(
				entity.getId(),
				entity.getCustomerId(),
				entity.getTitle(),
				entity.getDescription(),
				entity.getStatus(),
				entity.getPriority(),
				entity.getScheduledDate(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
