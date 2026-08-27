package com.oneilsoftware.sbp.module.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
		classes = JobModuleIntegrationTests.TestApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.jpa.hibernate.ddl-auto=validate")
class JobModuleIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private CustomerOperations customerOperations;

	@Autowired
	private JobOperations jobOperations;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@LocalServerPort
	private int serverPort;

	@Test
	void jobOwnsItsMigrationsAndTablesWithoutACrossModuleForeignKey() {
		List<String> migrationScripts = jdbcTemplate.queryForList(
				"select script from flyway_schema_history order by installed_rank",
				String.class);
		Integer crossModuleForeignKeys = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.table_constraints
				where table_schema = 'public'
				  and table_name = 'sbp_job'
				  and constraint_type = 'FOREIGN KEY'
				""", Integer.class);

		assertThat(migrationScripts).containsExactly(
				"V1__create_sbp_customer.sql",
				"V14__create_sbp_job.sql",
				"V15__create_sbp_job_audit_event.sql");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_job')",
				String.class)).isEqualTo("sbp_job");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_job_audit_event')",
				String.class)).isEqualTo("sbp_job_audit_event");
		assertThat(crossModuleForeignKeys).isZero();
	}

	@Test
	void jobCreatesPersistsReloadsAndListsByCustomer() {
		Customer customer = customerOperations.create(new CreateCustomer("Northwind Repair"));
		Customer anotherCustomer = customerOperations.create(new CreateCustomer("Contoso Repair"));

		Job created = jobOperations.create(new CreateJob(
				customer.id(),
				"  Replace water heater  ",
				"  Annual tune-up and inspection  ",
				JobPriority.HIGH,
				LocalDate.of(2026, 9, 15)));
		jobOperations.create(new CreateJob(
				anotherCustomer.id(),
				"Replace furnace filter",
				null,
				null,
				null));

		String persistedTitle = jdbcTemplate.queryForObject(
				"select title from sbp_job where id = ?",
				String.class,
				created.id());

		assertThat(created.status()).isEqualTo(JobStatus.OPEN);
		assertThat(created.priority()).isEqualTo(JobPriority.HIGH);
		assertThat(created.title()).isEqualTo("Replace water heater");
		assertThat(persistedTitle).isEqualTo("Replace water heater");
		assertThat(jobOperations.findById(created.id())).contains(created);
		assertThat(jobOperations.listByCustomer(customer.id())).containsExactly(created);
	}

	@Test
	void jobDefaultsToOpenNormalPriorityAndNoScheduledDate() {
		Customer customer = customerOperations.create(new CreateCustomer("Fabrikam Field Service"));

		Job created = jobOperations.create(new CreateJob(
				customer.id(),
				"Annual inspection",
				null,
				null,
				null));

		assertThat(created.status()).isEqualTo(JobStatus.OPEN);
		assertThat(created.priority()).isEqualTo(JobPriority.NORMAL);
		assertThat(created.description()).isNull();
		assertThat(created.scheduledDate()).isNull();
	}

	@Test
	void jobRejectsAnUnknownCustomerBeforePersistence() {
		UUID unknownCustomerId = UUID.randomUUID();
		Integer jobsBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_job",
				Integer.class);

		assertThatThrownBy(() -> jobOperations.create(new CreateJob(
				unknownCustomerId,
				"Work for a missing customer",
				null,
				null,
				null)))
				.isInstanceOf(UnknownCustomerException.class)
				.hasMessageContaining(unknownCustomerId.toString());
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_job",
				Integer.class)).isEqualTo(jobsBefore);
	}

	@Test
	void jobLifecycleEnforcesAllowedTransitions() {
		Customer customer = customerOperations.create(new CreateCustomer("Tailspin Services"));
		Job job = jobOperations.create(new CreateJob(customer.id(), "Service the AC unit", null, null, null));

		Job started = jobOperations.start(job.id());
		assertThat(started.status()).isEqualTo(JobStatus.IN_PROGRESS);

		Job completed = jobOperations.complete(job.id());
		assertThat(completed.status()).isEqualTo(JobStatus.COMPLETED);

		assertThatThrownBy(() -> jobOperations.complete(job.id()))
				.isInstanceOf(InvalidJobTransitionException.class);
		assertThatThrownBy(() -> jobOperations.cancel(job.id()))
				.isInstanceOf(InvalidJobTransitionException.class);
	}

	@Test
	void jobCancelIsAllowedFromOpenAndInProgressButNotAfterCompletion() {
		Customer customer = customerOperations.create(new CreateCustomer("Proseware Services"));
		Job open = jobOperations.create(new CreateJob(customer.id(), "Cancel me while open", null, null, null));
		assertThat(jobOperations.cancel(open.id()).status()).isEqualTo(JobStatus.CANCELLED);

		Job inProgress = jobOperations.create(new CreateJob(customer.id(), "Cancel me in progress", null, null, null));
		jobOperations.start(inProgress.id());
		assertThat(jobOperations.cancel(inProgress.id()).status()).isEqualTo(JobStatus.CANCELLED);

		Job completed = jobOperations.create(new CreateJob(customer.id(), "Too late to cancel", null, null, null));
		jobOperations.start(completed.id());
		jobOperations.complete(completed.id());
		assertThatThrownBy(() -> jobOperations.cancel(completed.id()))
				.isInstanceOf(InvalidJobTransitionException.class);
	}

	@Test
	void jobUpdateChangesEditableFieldsAndKeepsLifecycleState() {
		Customer customer = customerOperations.create(new CreateCustomer("Wingtip Repair"));
		Job job = jobOperations.create(new CreateJob(customer.id(), "Original title", "Original description", JobPriority.LOW, null));
		jobOperations.start(job.id());

		Job updated = jobOperations.update(job.id(), new UpdateJob(
				"Updated title",
				null,
				JobPriority.HIGH,
				LocalDate.of(2026, 10, 1)));

		assertThat(updated.title()).isEqualTo("Updated title");
		assertThat(updated.description()).isNull();
		assertThat(updated.priority()).isEqualTo(JobPriority.HIGH);
		assertThat(updated.scheduledDate()).isEqualTo(LocalDate.of(2026, 10, 1));
		assertThat(updated.status()).isEqualTo(JobStatus.IN_PROGRESS);
		assertThat(jobOperations.findById(job.id())).contains(updated);
	}

	@Test
	void jobUpdateRejectsAnUnknownJob() {
		UUID unknownJobId = UUID.randomUUID();
		assertThatThrownBy(() -> jobOperations.update(unknownJobId, new UpdateJob("New title", null, null, null)))
				.isInstanceOf(UnknownJobException.class);
	}

	@Test
	void jobSearchFiltersAndPaginates() {
		Customer customerA = customerOperations.create(new CreateCustomer("A Repair"));
		Customer customerB = customerOperations.create(new CreateCustomer("B Repair"));

		Job jobA = jobOperations.create(new CreateJob(customerA.id(), "Fix the roof leak", "Urgent leak", JobPriority.HIGH, null));
		Job jobB = jobOperations.create(new CreateJob(customerA.id(), "Paint the fence", "Spring painting", JobPriority.LOW, null));
		Job jobC = jobOperations.create(new CreateJob(customerB.id(), "Fix the gate hinge", null, JobPriority.NORMAL, null));
		Job jobCStarted = jobOperations.start(jobC.id());

		// Text query matches title and description case-insensitively. Assertions are scoped to a
		// customer because the shared Testcontainers database accumulates jobs from other test
		// methods; scoping keeps them deterministic regardless of test order.
		assertThat(jobOperations.search(new JobFilter("leak", customerA.id(), null, null, 0, 20)).items())
				.containsExactly(jobA);
		assertThat(jobOperations.search(new JobFilter("SPRING", customerA.id(), null, null, 0, 20)).items())
				.containsExactly(jobB);

		// Customer, status, and priority filters narrow the list.
		assertThat(jobOperations.search(new JobFilter(null, customerA.id(), null, null, 0, 20)).items())
				.containsExactly(jobB, jobA);
		assertThat(jobOperations.search(new JobFilter(null, customerB.id(), JobStatus.IN_PROGRESS, null, 0, 20)).items())
				.containsExactly(jobCStarted);
		assertThat(jobOperations.search(new JobFilter(null, customerA.id(), null, JobPriority.HIGH, 0, 20)).items())
				.containsExactly(jobA);

		// Pagination with deterministic newest-first ordering.
		JobPage page = jobOperations.search(new JobFilter(null, customerA.id(), null, null, 0, 1));
		assertThat(page.items()).containsExactly(jobB);
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(1);

		JobPage secondPage = jobOperations.search(new JobFilter(null, customerA.id(), null, null, 1, 1));
		assertThat(secondPage.items()).containsExactly(jobA);
	}

	@Test
	void jobAuditRecordsCreateUpdateAndStatusChangesWithFieldNamesOnly() {
		Customer customer = customerOperations.create(new CreateCustomer("Audit Co"));
		Job job = jobOperations.create(new CreateJob(customer.id(), "Audited job", null, JobPriority.HIGH, null));
		jobOperations.update(job.id(), new UpdateJob("Audited job", "Now described", JobPriority.HIGH, null));
		jobOperations.start(job.id());

		List<String> actions = jdbcTemplate.queryForList(
				"select action from sbp_job_audit_event where subject_type = 'JOB' and subject_id = ? order by occurred_at",
				String.class,
				job.id().toString());
		assertThat(actions).containsExactly("JOB_CREATED", "JOB_UPDATED", "JOB_STATUS_CHANGED");

		// The module default actor is "system" in this composition (no customer resolver supplied).
		List<String> actors = jdbcTemplate.queryForList(
				"select distinct actor from sbp_job_audit_event where subject_id = ?",
				String.class,
				job.id().toString());
		assertThat(actors).containsExactly("system");

		String changedFields = jdbcTemplate.queryForObject(
				"select changed_fields from sbp_job_audit_event where subject_id = ? and action = 'JOB_UPDATED'",
				String.class,
				job.id().toString());
		assertThat(changedFields).contains("description");

		String detail = jdbcTemplate.queryForObject(
				"select detail from sbp_job_audit_event where subject_id = ? and action = 'JOB_STATUS_CHANGED'",
				String.class,
				job.id().toString());
		assertThat(detail).isEqualTo("OPEN→IN_PROGRESS");
	}

	@Test
	void restApiCreatesGetsListsAndLifecyclesJobs() {
		Customer customer = customerOperations.create(new CreateCustomer("Rest Api Repair"));
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		ResponseEntity<Job> createResponse = client.post()
				.uri("/api/jobs")
				.body(new CreateRequest(
						customer.id(),
						"Replace the pump",
						"Circulation pump is noisy",
						JobPriority.HIGH,
						LocalDate.of(2026, 9, 20)))
				.retrieve()
				.toEntity(Job.class);
		Job created = createResponse.getBody();
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created).isNotNull();

		Job retrieved = client.get()
				.uri("/api/jobs/{jobId}", created.id())
				.retrieve()
				.body(Job.class);
		assertThat(retrieved).isEqualTo(created);

		JobPage page = client.get()
				.uri("/api/jobs?customerId={customerId}&page=0&size=10", customer.id())
				.retrieve()
				.body(JobPage.class);
		assertThat(page).isNotNull();
		assertThat(page.items()).containsExactly(created);
		assertThat(page.totalElements()).isEqualTo(1);

		Job started = client.post()
				.uri("/api/jobs/{jobId}/start", created.id())
				.retrieve()
				.body(Job.class);
		assertThat(started.status()).isEqualTo(JobStatus.IN_PROGRESS);

		Job completed = client.post()
				.uri("/api/jobs/{jobId}/complete", created.id())
				.retrieve()
				.body(Job.class);
		assertThat(completed.status()).isEqualTo(JobStatus.COMPLETED);

		Job[] customerJobs = client.get()
				.uri("/api/customers/{customerId}/jobs", customer.id())
				.retrieve()
				.body(Job[].class);
		assertThat(customerJobs).containsExactly(completed);
	}

	@Test
	void restApiRejectsInvalidTransitionsAndUnknownReferences() {
		Customer customer = customerOperations.create(new CreateCustomer("Conflict Repair"));
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		Job created = client.post()
				.uri("/api/jobs")
				.body(new CreateRequest(customer.id(), "Never completes", null, null, null))
				.retrieve()
				.body(Job.class);
		assertThat(created).isNotNull();

		ResponseEntity<Void> invalidTransition = client.post()
				.uri("/api/jobs/{jobId}/complete", created.id())
				.retrieve()
				.onStatus(s -> s.value() == 409, (request, response) -> {
				})
				.toBodilessEntity();
		assertThat(invalidTransition.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		ResponseEntity<Void> unknownJob = client.get()
				.uri("/api/jobs/{jobId}", UUID.randomUUID())
				.retrieve()
				.onStatus(s -> s.value() == 404, (request, response) -> {
				})
				.toBodilessEntity();
		assertThat(unknownJob.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<Void> unknownCustomer = client.post()
				.uri("/api/jobs")
				.body(new CreateRequest(UUID.randomUUID(), "No such customer", null, null, null))
				.retrieve()
				.onStatus(s -> s.value() == 404, (request, response) -> {
				})
				.toBodilessEntity();
		assertThat(unknownCustomer.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void jobAuditEventsAreReadablePerJobNewestFirstAndFilteredByAction() {
		Customer customer = customerOperations.create(new CreateCustomer("Readable Audit Co"));
		Job job = jobOperations.create(new CreateJob(customer.id(), "Readable audit job", null, JobPriority.HIGH, null));
		jobOperations.update(job.id(), new UpdateJob("Readable audit job", "Now described", JobPriority.HIGH, null));
		jobOperations.start(job.id());

		List<JobAuditEvent> events = jobOperations.listAuditEvents(job.id(), null);
		assertThat(events).hasSize(3);
		assertThat(events).extracting(JobAuditEvent::action)
				.containsExactlyInAnyOrder("JOB_CREATED", "JOB_UPDATED", "JOB_STATUS_CHANGED");
		assertThat(events.get(0).occurredAt())
				.isAfterOrEqualTo(events.get(events.size() - 1).occurredAt());

		JobAuditEvent updated = events.stream()
				.filter(event -> event.action().equals("JOB_UPDATED"))
				.findFirst().orElseThrow();
		assertThat(updated.actor()).isEqualTo("system");
		assertThat(updated.changedFields()).containsExactly("description");
		assertThat(updated.detail()).isNull();

		JobAuditEvent statusChanged = events.stream()
				.filter(event -> event.action().equals("JOB_STATUS_CHANGED"))
				.findFirst().orElseThrow();
		assertThat(statusChanged.changedFields()).containsExactly("status");
		assertThat(statusChanged.detail()).isEqualTo("OPEN→IN_PROGRESS");

		assertThat(jobOperations.listAuditEvents(job.id(), "JOB_STATUS_CHANGED"))
				.extracting(JobAuditEvent::action)
				.containsExactly("JOB_STATUS_CHANGED");
		// A blank action normalizes to "no filter", so all events are returned.
		assertThat(jobOperations.listAuditEvents(job.id(), "   ")).hasSize(3);
		// An unknown action matches nothing.
		assertThat(jobOperations.listAuditEvents(job.id(), "JOB_DELETED")).isEmpty();

		assertThatThrownBy(() -> jobOperations.listAuditEvents(UUID.randomUUID(), null))
				.isInstanceOf(UnknownJobException.class);
	}

	@Test
	void restApiExposesPerJobAuditEvents() {
		Customer customer = customerOperations.create(new CreateCustomer("Rest Audit Co"));
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		Job created = client.post()
				.uri("/api/jobs")
				.body(new CreateRequest(customer.id(), "Audit via rest", null, null, null))
				.retrieve()
				.body(Job.class);
		assertThat(created).isNotNull();
		client.post().uri("/api/jobs/{jobId}/start", created.id()).retrieve().toBodilessEntity();

		JobAuditEvent[] events = client.get()
				.uri("/api/jobs/{jobId}/audit-events", created.id())
				.retrieve()
				.body(JobAuditEvent[].class);
		assertThat(events).extracting(JobAuditEvent::action)
				.containsExactly("JOB_STATUS_CHANGED", "JOB_CREATED");

		JobAuditEvent[] filtered = client.get()
				.uri("/api/jobs/{jobId}/audit-events?action=JOB_CREATED", created.id())
				.retrieve()
				.body(JobAuditEvent[].class);
		assertThat(filtered).extracting(JobAuditEvent::action).containsExactly("JOB_CREATED");

		ResponseEntity<Void> unknown = client.get()
				.uri("/api/jobs/{jobId}/audit-events", UUID.randomUUID())
				.retrieve()
				.onStatus(s -> s.value() == 404, (request, response) -> {
				})
				.toBodilessEntity();
		assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void jobSearchPaginatesAtVolumeAgainstThePerformanceExpectation() {
		Customer customer = customerOperations.create(new CreateCustomer("Volume Repair"));
		int total = 1050;
		for (int i = 0; i < total; i++) {
			jobOperations.create(new CreateJob(
					customer.id(),
					"Volume job " + i,
					null,
					i % 3 == 0 ? JobPriority.HIGH : JobPriority.NORMAL,
					null));
		}

		JobPage firstPage = jobOperations.search(new JobFilter(null, customer.id(), null, null, 0, 20));
		assertThat(firstPage.totalElements()).isEqualTo(total);
		assertThat(firstPage.totalPages()).isEqualTo(53);
		assertThat(firstPage.items()).hasSize(20);
		assertThat(firstPage.items().get(0).title()).isEqualTo("Volume job " + (total - 1));
		assertThat(firstPage.items().get(19).title()).isEqualTo("Volume job " + (total - 20));

		JobPage lastPage = jobOperations.search(new JobFilter(null, customer.id(), null, null, 52, 20));
		assertThat(lastPage.items()).hasSize(10);

		JobPage beyond = jobOperations.search(new JobFilter(null, customer.id(), null, null, 99, 20));
		assertThat(beyond.items()).isEmpty();
		assertThat(beyond.totalElements()).isEqualTo(total);

		// Filters still narrow at volume.
		JobPage high = jobOperations.search(new JobFilter(null, customer.id(), null, JobPriority.HIGH, 0, 100));
		assertThat(high.totalElements()).isEqualTo(350);
	}

	@Test
	void jobAuditDoesNotRecordFailedOperations() {
		Customer customer = customerOperations.create(new CreateCustomer("No Failed Audit Co"));
		Job job = jobOperations.create(new CreateJob(customer.id(), "Never completes", null, null, null));

		// An invalid transition throws before any state change, so no audit event is written.
		assertThatThrownBy(() -> jobOperations.complete(job.id()))
				.isInstanceOf(InvalidJobTransitionException.class);
		List<String> actions = jdbcTemplate.queryForList(
				"select action from sbp_job_audit_event where subject_id = ? order by occurred_at",
				String.class,
				job.id().toString());
		assertThat(actions).containsExactly("JOB_CREATED");

		// A rejected create for an unknown customer writes nothing either.
		Integer eventsBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_job_audit_event",
				Integer.class);
		assertThatThrownBy(() -> jobOperations.create(new CreateJob(
				UUID.randomUUID(), "No such customer", null, null, null)))
				.isInstanceOf(UnknownCustomerException.class);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_job_audit_event",
				Integer.class)).isEqualTo(eventsBefore);
	}

	private record CreateRequest(
			UUID customerId,
			String title,
			String description,
			JobPriority priority,
			LocalDate scheduledDate) {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}
}
