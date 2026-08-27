package com.oneilsoftware.sbp.module.job.internal.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.job.api.CreateJob;
import com.oneilsoftware.sbp.module.job.api.InvalidJobTransitionException;
import com.oneilsoftware.sbp.module.job.api.Job;
import com.oneilsoftware.sbp.module.job.api.JobAuditEvent;
import com.oneilsoftware.sbp.module.job.api.JobFilter;
import com.oneilsoftware.sbp.module.job.api.JobOperations;
import com.oneilsoftware.sbp.module.job.api.JobPage;
import com.oneilsoftware.sbp.module.job.api.JobPriority;
import com.oneilsoftware.sbp.module.job.api.JobStatus;
import com.oneilsoftware.sbp.module.job.api.UnknownCustomerException;
import com.oneilsoftware.sbp.module.job.api.UnknownJobException;
import com.oneilsoftware.sbp.module.job.api.UpdateJob;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

	private final JobOperations jobOperations;

	public JobController(JobOperations jobOperations) {
		this.jobOperations = jobOperations;
	}

	@PostMapping("/api/jobs")
	public ResponseEntity<Job> create(@Valid @RequestBody CreateJobRequest request) {
		Job job = jobOperations.create(new CreateJob(
				request.customerId(),
				request.title(),
				request.description(),
				request.priority(),
				request.scheduledDate()));
		return ResponseEntity.created(URI.create("/api/jobs/" + job.id())).body(job);
	}

	@GetMapping("/api/jobs")
	public JobPage list(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) UUID customerId,
			@RequestParam(required = false) JobStatus status,
			@RequestParam(required = false) JobPriority priority,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		return jobOperations.search(new JobFilter(
				query,
				customerId,
				status,
				priority,
				page == null ? 0 : page,
				size == null ? JobFilter.DEFAULT_PAGE_SIZE : size));
	}

	@GetMapping("/api/jobs/{jobId}")
	public ResponseEntity<Job> get(@PathVariable UUID jobId) {
		return ResponseEntity.of(jobOperations.findById(jobId));
	}

	@PutMapping("/api/jobs/{jobId}")
	public Job update(@PathVariable UUID jobId, @Valid @RequestBody UpdateJobRequest request) {
		return jobOperations.update(jobId, new UpdateJob(
				request.title(),
				request.description(),
				request.priority(),
				request.scheduledDate()));
	}

	@PostMapping("/api/jobs/{jobId}/start")
	public Job start(@PathVariable UUID jobId) {
		return jobOperations.start(jobId);
	}

	@PostMapping("/api/jobs/{jobId}/complete")
	public Job complete(@PathVariable UUID jobId) {
		return jobOperations.complete(jobId);
	}

	@PostMapping("/api/jobs/{jobId}/cancel")
	public Job cancel(@PathVariable UUID jobId) {
		return jobOperations.cancel(jobId);
	}

	@GetMapping("/api/jobs/{jobId}/audit-events")
	public List<JobAuditEvent> auditEvents(
			@PathVariable UUID jobId,
			@RequestParam(required = false) String action) {
		return jobOperations.listAuditEvents(jobId, action);
	}

	@GetMapping("/api/customers/{customerId}/jobs")
	public List<Job> listByCustomer(@PathVariable UUID customerId) {
		return jobOperations.listByCustomer(customerId);
	}

	@ExceptionHandler({UnknownJobException.class, UnknownCustomerException.class})
	ResponseEntity<Void> unknownJobOrCustomer() {
		return ResponseEntity.notFound().build();
	}

	@ExceptionHandler(InvalidJobTransitionException.class)
	ResponseEntity<Void> invalidTransition() {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}
}
