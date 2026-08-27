package com.acmeco.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.acmeco.reference.customer.api.AcmeCustomerView;
import com.acmeco.reference.customer.api.SaveAcmeCustomerProfile;
import com.acmeco.reference.customer.internal.application.AcmeCustomerProfileService;
import com.acmeco.reference.security.AcmeCredentialStatus;
import com.oneilsoftware.sbp.module.contact.api.Contact;
import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.api.CreateContact;
import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.document.api.Document;
import com.oneilsoftware.sbp.module.document.api.DocumentAuditEvent;
import com.oneilsoftware.sbp.module.document.api.DocumentPage;
import com.oneilsoftware.sbp.module.job.api.Job;
import com.oneilsoftware.sbp.module.job.api.JobAuditEvent;
import com.oneilsoftware.sbp.module.job.api.JobPage;
import com.oneilsoftware.sbp.module.job.api.JobStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.Group;
import com.oneilsoftware.sbp.module.usermaintenance.api.Permission;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserAuditEvent;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.jpa.hibernate.ddl-auto=validate",
				"acme.security.username=test-user",
				"acme.security.password=test-password",
				"sbp.usermaintenance.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
				"sbp.usermaintenance.search-key=ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA="
		})
class AcmeReferenceApplicationTests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private CustomerOperations customerOperations;

	@Autowired
	private ContactOperations contactOperations;

	@Autowired
	private AcmeCustomerProfileService profileService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@LocalServerPort
	private int serverPort;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Test
	void customerAcmeAndContactMigrationsApplyWithoutChangingCustomerTable() {
		List<String> migrationScripts = jdbcTemplate.queryForList(
				"select script from flyway_schema_history order by installed_rank",
				String.class);
		List<String> customerColumns = jdbcTemplate.queryForList("""
				select column_name
				from information_schema.columns
				where table_schema = 'public' and table_name = 'sbp_customer'
				order by ordinal_position
				""", String.class);

		assertThat(migrationScripts).containsExactly(
				"V1__create_sbp_customer.sql",
				"V2__create_acme_customer_profile.sql",
				"V3__create_sbp_contact.sql",
				"V4__create_sbp_user.sql",
				"V5__create_sbp_role_and_user_role.sql",
				"V6__create_sbp_group_and_user_group.sql",
				"V7__create_sbp_user_audit_event.sql",
				"V8__widen_sbp_user_pii_columns.sql",
				"V9__add_sbp_user_search_tokens.sql",
				"V10__create_sbp_permission_and_role_permission.sql",
				"V11__create_acme_user_credential.sql",
				"V12__add_acme_password_change_required.sql",
				"V13__create_sbp_group_role.sql",
				"V14__create_sbp_job.sql",
				"V15__create_sbp_job_audit_event.sql",
				"V16__create_sbp_document.sql",
				"V17__create_sbp_document_content.sql",
				"V18__create_sbp_document_audit_event.sql");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.acme_customer_profile')",
				String.class)).isEqualTo("acme_customer_profile");
		assertThat(customerColumns).containsExactly("id", "name", "status");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_contact')",
				String.class)).isEqualTo("sbp_contact");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_user')",
				String.class)).isEqualTo("sbp_user");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_role')",
				String.class)).isEqualTo("sbp_role");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_user_role')",
				String.class)).isEqualTo("sbp_user_role");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_group')",
				String.class)).isEqualTo("sbp_group");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_user_group')",
				String.class)).isEqualTo("sbp_user_group");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_user_audit_event')",
				String.class)).isEqualTo("sbp_user_audit_event");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_group_role')",
				String.class)).isEqualTo("sbp_group_role");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_job')",
				String.class)).isEqualTo("sbp_job");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_job_audit_event')",
				String.class)).isEqualTo("sbp_job_audit_event");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_document')",
				String.class)).isEqualTo("sbp_document");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_document_content')",
				String.class)).isEqualTo("sbp_document_content");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_document_audit_event')",
				String.class)).isEqualTo("sbp_document_audit_event");
	}

	@Test
	void acmeProfilePersistsUpdatesAndComposesThroughCustomerPublicApi() {
		Customer customer = customerOperations.create(new CreateCustomer("Northwind Repair"));

		profileService.save(customer.id(), new SaveAcmeCustomerProfile(
				"North District",
				"Call before dispatch"));
		AcmeCustomerView updated = profileService.save(customer.id(), new SaveAcmeCustomerProfile(
				"Central District",
				"Use the loading entrance")).orElseThrow();

		String persistedRegion = jdbcTemplate.queryForObject(
				"select service_region from acme_customer_profile where customer_id = ?",
				String.class,
				customer.id());
		AcmeCustomerView reloaded = profileService.findByCustomerId(customer.id()).orElseThrow();

		assertThat(updated.customer()).isEqualTo(customer);
		assertThat(persistedRegion).isEqualTo("Central District");
		assertThat(reloaded).isEqualTo(updated);
	}

	@Test
	void combinedRestApiCreatesProfileAndReturnsAcmeCustomerView() {
		RestClient client = authenticatedClient();
		Customer customer = client.post()
				.uri("/api/customers")
				.body(new CreateCustomerRequest("Contoso Field Service"))
				.retrieve()
				.body(Customer.class);
		assertThat(customer).isNotNull();

		ResponseEntity<AcmeCustomerView> saveResponse = client.put()
				.uri("/api/acme/customers/{customerId}/profile", customer.id())
				.body(new SaveProfileRequest("Coastal", "Gate code is on file"))
				.retrieve()
				.toEntity(AcmeCustomerView.class);
		AcmeCustomerView reloaded = client.get()
				.uri("/api/acme/customers/{customerId}", customer.id())
				.retrieve()
				.body(AcmeCustomerView.class);

		assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(reloaded).isEqualTo(saveResponse.getBody());
		assertThat(reloaded.customer()).isEqualTo(customer);
		assertThat(reloaded.profile().serviceRegion()).isEqualTo("Coastal");
	}

	@Test
	void combinedViewReturnsProfileLessCustomerForNewRecordsAnd404ForUnknown() {
		RestClient client = authenticatedClient();
		Customer customer = client.post()
				.uri("/api/customers")
				.body(new CreateCustomerRequest("Blank Profile Co"))
				.retrieve()
				.body(Customer.class);
		assertThat(customer).isNotNull();

		ResponseEntity<AcmeCustomerView> view = client.get()
				.uri("/api/acme/customers/{customerId}", customer.id())
				.retrieve()
				.toEntity(AcmeCustomerView.class);
		assertThat(view.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(view.getBody()).isNotNull();
		assertThat(view.getBody().customer()).isEqualTo(customer);
		assertThat(view.getBody().profile()).isNull();

		ResponseEntity<Void> unknown = client.get()
				.uri("/api/acme/customers/{customerId}", UUID.randomUUID())
				.retrieve()
				.onStatus(s -> s.value() == 404, (request, response) -> {
				})
				.toBodilessEntity();
		assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void contactAndAcmeProfileRemainSeparateCustomerCompositions() {
		Customer customer = customerOperations.create(new CreateCustomer("Tailspin Services"));
		Contact contact = contactOperations.create(new CreateContact(
				customer.id(),
				"Jordan Lee",
				"jordan@example.com",
				"555-0104",
				"Operations Manager"));
		profileService.save(customer.id(), new SaveAcmeCustomerProfile(
				"Western District",
				"Quarterly service review"));

		assertThat(contactOperations.listByCustomer(customer.id())).containsExactly(contact);
		assertThat(profileService.findByCustomerId(customer.id()).orElseThrow().profile().serviceRegion())
				.isEqualTo("Western District");
	}

	@Test
	void restApiRejectsUnauthenticatedRequests() {
		ResponseEntity<Void> response = RestClient.create("http://localhost:" + serverPort)
				.get()
				.uri("/api/customers")
				.retrieve()
				.onStatus(status -> status.value() == 401, (request, rejectedResponse) -> {
				})
				.toBodilessEntity();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders().get("WWW-Authenticate")).isNull();
	}

	@Test
	void authenticatedSessionEndpointIdentifiesTheDemoUser() {
		ResponseEntity<String> response = authenticatedClient().get()
				.uri("/api/auth/session")
				.retrieve()
				.toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("{\"username\":\"test-user\",\"passwordChangeRequired\":false}");
	}

	@Test
	void userMaintenanceWorksThroughTheAuthenticatedAcmeApi() {
		RestClient client = authenticatedClient();

		ResponseEntity<User> createResponse = client.post()
				.uri("/api/users")
				.body(new CreateUserRequest(
						"acme.admin",
						"admin@acme.example",
						"Acme",
						"Admin",
						null))
				.retrieve()
				.toEntity(User.class);
		User created = createResponse.getBody();
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created).isNotNull();

		User[] users = client.get()
				.uri("/api/users?query=acme.admin")
				.retrieve()
				.body(User[].class);
		assertThat(users).hasSize(1);
		assertThat(users[0].username()).isEqualTo("acme.admin");
		assertThat(users[0].email()).isEqualTo("a***@acme.example");

		User deactivated = client.post()
				.uri("/api/users/{userId}/deactivate", created.id())
				.retrieve()
				.body(User.class);
		assertThat(deactivated.status()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	void rolesAndUserAssignmentsWorkThroughTheAuthenticatedAcmeApi() {
		RestClient client = authenticatedClient();

		Role role = client.post()
				.uri("/api/roles")
				.body(new CreateRoleRequest("Manager", "Oversees daily work"))
				.retrieve()
				.body(Role.class);
		assertThat(role).isNotNull();

		User user = client.post()
				.uri("/api/users")
				.body(new CreateUserRequest("acme.manager", "manager@acme.example", "Acme", "Manager", null))
				.retrieve()
				.body(User.class);
		assertThat(user).isNotNull();

		Role[] assigned = client.post()
				.uri("/api/users/{userId}/roles/{roleId}", user.id(), role.id())
				.retrieve()
				.body(Role[].class);
		assertThat(assigned).containsExactly(role);

		Role[] removed = client.delete()
				.uri("/api/users/{userId}/roles/{roleId}", user.id(), role.id())
				.retrieve()
				.body(Role[].class);
		assertThat(removed).isEmpty();
	}

	@Test
	void groupsAndUserMembershipWorkThroughTheAuthenticatedAcmeApi() {
		RestClient client = authenticatedClient();

		Group group = client.post()
				.uri("/api/groups")
				.body(new CreateGroupRequest("Field Crew", "Field service team"))
				.retrieve()
				.body(Group.class);
		assertThat(group).isNotNull();

		User user = client.post()
				.uri("/api/users")
				.body(new CreateUserRequest("acme.technician", "tech@acme.example", "Acme", "Technician", null))
				.retrieve()
				.body(User.class);
		assertThat(user).isNotNull();

		Group[] assigned = client.post()
				.uri("/api/users/{userId}/groups/{groupId}", user.id(), group.id())
				.retrieve()
				.body(Group[].class);
		assertThat(assigned).containsExactly(group);

		User[] members = client.get()
				.uri("/api/groups/{groupId}/members", group.id())
				.retrieve()
				.body(User[].class);
		assertThat(members).containsExactly(user);

		Group[] removed = client.delete()
				.uri("/api/users/{userId}/groups/{groupId}", user.id(), group.id())
				.retrieve()
				.body(Group[].class);
		assertThat(removed).isEmpty();
	}

	@Test
	void selfServiceProfileMapsTheAuthenticatedUserAndEditsPermittedFields() {
		RestClient client = authenticatedClient();

		User me = client.get()
				.uri("/api/me")
				.retrieve()
				.body(User.class);
		assertThat(me).isNotNull();
		assertThat(me.username()).isEqualTo("test-user");
		assertThat(me.email()).isEqualTo("test-user@acme.example");

		User updated = client.put()
				.uri("/api/me")
				.body(new UpdateMyProfileRequest(
						"updated@acme.example",
						"Acme",
						"Administrator",
						"555-0101"))
				.retrieve()
				.body(User.class);
		assertThat(updated.email()).isEqualTo("updated@acme.example");
		assertThat(updated.username()).isEqualTo("test-user");
		assertThat(updated.status()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void auditEventsCarryTheAuthenticatedActor() {
		RestClient client = authenticatedClient();

		User user = client.post()
				.uri("/api/users")
				.body(new CreateUserRequest(
						"audit.acme",
						"audit.acme@example.com",
						"Audit",
						"Acme",
						null))
				.retrieve()
				.body(User.class);
		assertThat(user).isNotNull();

		UserAuditEvent[] events = client.get()
				.uri("/api/audit?subjectType=USER&subjectId={id}", user.id())
				.retrieve()
				.body(UserAuditEvent[].class);

		assertThat(events).isNotEmpty();
		assertThat(events).allSatisfy(event -> assertThat(event.actor()).isEqualTo("test-user"));
	}

	@Test
	void jobWorkflowWorksThroughTheAuthenticatedAcmeApiWithTheAuthenticatedActor() {
		RestClient client = authenticatedClient();

		Customer customer = client.post()
				.uri("/api/customers")
				.body(new CreateCustomerRequest("Job Workflow Repair"))
				.retrieve()
				.body(Customer.class);
		assertThat(customer).isNotNull();

		ResponseEntity<Job> createResponse = client.post()
				.uri("/api/jobs")
				.body(new CreateJobRequest(
						customer.id(),
						"Repair the garage door",
						"Spring tension is off",
						"HIGH",
						"2026-09-25"))
				.retrieve()
				.toEntity(Job.class);
		Job created = createResponse.getBody();
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created).isNotNull();
		assertThat(created.status()).isEqualTo(JobStatus.OPEN);

		JobPage page = client.get()
				.uri("/api/jobs?customerId={customerId}&page=0&size=10", customer.id())
				.retrieve()
				.body(JobPage.class);
		assertThat(page).isNotNull();
		assertThat(page.items()).containsExactly(created);
		assertThat(page.totalElements()).isEqualTo(1);

		// Edit the work order, then verify the change persists on reload.
		Job edited = client.put()
				.uri("/api/jobs/{jobId}", created.id())
				.body(new UpdateJobRequest(
						"Repair the garage door (urgent)",
						"Spring tension is off",
						"HIGH",
						"2026-09-26"))
				.retrieve()
				.body(Job.class);
		assertThat(edited).isNotNull();
		assertThat(edited.title()).isEqualTo("Repair the garage door (urgent)");

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

		Job reloaded = client.get()
				.uri("/api/jobs/{jobId}", created.id())
				.retrieve()
				.body(Job.class);
		assertThat(reloaded.title()).isEqualTo("Repair the garage door (urgent)");
		assertThat(reloaded.scheduledDate()).isEqualTo(LocalDate.of(2026, 9, 26));
		assertThat(reloaded.status()).isEqualTo(JobStatus.COMPLETED);

		List<String> actions = jdbcTemplate.queryForList(
				"select action from sbp_job_audit_event where subject_type = 'JOB' and subject_id = ? order by occurred_at",
				String.class,
				created.id().toString());
		assertThat(actions).containsExactly(
				"JOB_CREATED",
				"JOB_UPDATED",
				"JOB_STATUS_CHANGED",
				"JOB_STATUS_CHANGED");

		// The customer-owned AcmeAuditActorResolver overrides the module default, so the actor is
		// the authenticated user rather than "system".
		List<String> actors = jdbcTemplate.queryForList(
				"select distinct actor from sbp_job_audit_event where subject_id = ?",
				String.class,
				created.id().toString());
		assertThat(actors).containsExactly("test-user");

		// The per-Job audit read surface (JOB-FR-09) exposes the same history through the API,
		// newest first, with the authenticated actor.
		JobAuditEvent[] auditEvents = client.get()
				.uri("/api/jobs/{jobId}/audit-events", created.id())
				.retrieve()
				.body(JobAuditEvent[].class);
		assertThat(auditEvents).extracting(JobAuditEvent::action)
				.containsExactly("JOB_STATUS_CHANGED", "JOB_STATUS_CHANGED", "JOB_UPDATED", "JOB_CREATED");
		assertThat(auditEvents).allSatisfy(event -> assertThat(event.actor()).isEqualTo("test-user"));
		assertThat(auditEvents).allSatisfy(event -> assertThat(event.changedFields()).isNotEmpty());
	}

	@Test
	void documentWorkflowWorksThroughTheAuthenticatedAcmeApiWithTheAuthenticatedActor() {
		RestClient client = authenticatedClient();

		Customer customer = client.post()
				.uri("/api/customers")
				.body(new CreateCustomerRequest("Document Workflow Repair"))
				.retrieve()
				.body(Customer.class);
		assertThat(customer).isNotNull();

		byte[] contractBytes = "SIGNED-CONTRACT-CONTENT".getBytes();
		ResponseEntity<Document> createResponse = client.post()
				.uri("/api/documents")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipart("signed-contract.pdf", contractBytes,
						"CUSTOMER", customer.id().toString()))
				.retrieve()
				.toEntity(Document.class);
		Document created = createResponse.getBody();
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created).isNotNull();
		assertThat(created.displayName()).isEqualTo("signed-contract.pdf");

		// Content is stored in the module's database-backed storage and downloads byte-identical.
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_content where storage_key = ?",
				Integer.class,
				created.id())).isEqualTo(1);
		ResponseEntity<byte[]> download = client.get()
				.uri("/api/documents/{documentId}/content", created.id())
				.retrieve()
				.toEntity(byte[].class);
		assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(download.getBody()).isEqualTo(contractBytes);
		assertThat(download.getHeaders().getFirst("Content-Disposition"))
				.contains("attachment").contains("signed-contract.pdf");

		// The Documents list supports the owner filter through the authenticated API.
		DocumentPage page = client.get()
				.uri("/api/documents?ownerType=CUSTOMER&ownerId={ownerId}&page=0&size=10", customer.id())
				.retrieve()
				.body(DocumentPage.class);
		assertThat(page).isNotNull();
		assertThat(page.items()).containsExactly(created);

		// Rename, replace, and delete complete the customer-owned workflow.
		Document renamed = client.put()
				.uri("/api/documents/{documentId}", created.id())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new RenameDocumentRequest("final-contract.pdf"))
				.retrieve()
				.body(Document.class);
		assertThat(renamed).isNotNull();
		assertThat(renamed.displayName()).isEqualTo("final-contract.pdf");

		byte[] replacement = "RE-SCANNED-CONTRACT".getBytes();
		client.put()
				.uri("/api/documents/{documentId}/content", created.id())
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipart("final-contract.pdf", replacement, null, null))
				.retrieve()
				.toBodilessEntity();
		assertThat(client.get()
				.uri("/api/documents/{documentId}/content", created.id())
				.retrieve()
				.body(byte[].class)).isEqualTo(replacement);

		client.delete()
				.uri("/api/documents/{documentId}", created.id())
				.retrieve()
				.toBodilessEntity();

		// Audit events carry the authenticated actor (the customer-owned resolver), newest first,
		// and remain readable after deletion.
		DocumentAuditEvent[] events = client.get()
				.uri("/api/documents/{documentId}/audit-events", created.id())
				.retrieve()
				.body(DocumentAuditEvent[].class);
		assertThat(events).extracting(DocumentAuditEvent::action)
				.containsExactlyInAnyOrder(
						"DOCUMENT_CREATED",
						"DOCUMENT_METADATA_UPDATED",
						"DOCUMENT_CONTENT_REPLACED",
						"DOCUMENT_DELETED");
		assertThat(events).allSatisfy(event -> assertThat(event.actor()).isEqualTo("test-user"));
		assertThat(events[0].occurredAt()).isAfterOrEqualTo(events[events.length - 1].occurredAt());
	}

	@Test
	void permissionCatalogIsSeededAndReadable() {
		RestClient client = authenticatedClient();

		Permission[] permissions = client.get()
				.uri("/api/permissions")
				.retrieve()
				.body(Permission[].class);
		assertThat(permissions).isNotEmpty();
		assertThat(permissions).extracting(Permission::key)
				.contains(
						"acme.customer.read",
						"acme.job.read",
						"acme.job.manage",
						"acme.document.read",
						"acme.document.manage",
						"acme.report.view");

		Role[] roles = client.get()
				.uri("/api/permissions/{key}/roles", "acme.customer.read")
				.retrieve()
				.body(Role[].class);
		assertThat(roles).extracting(Role::name).contains("Administrator");
	}

	@Test
	void selfServicePasswordChangeOperatesOnTheAuthenticatedUserOnly() {
		RestClient admin = authenticatedClient();
		User selfUser = admin.post()
				.uri("/api/users")
				.body(new CreateUserRequest("self.service", "self@example.com", "Self", "Service", null))
				.retrieve()
				.body(User.class);
		admin.put()
				.uri("/api/acme/users/{userId}/password", selfUser.id())
				.body(new ResetPasswordRequest("initial-pass-123", false))
				.retrieve()
				.toBodilessEntity();

		ResponseEntity<AcmeCredentialStatus> change = basicClient("self.service", "initial-pass-123").post()
				.uri("/api/me/password")
				.body(new ChangePasswordRequest("initial-pass-123", "changed-pass-456"))
				.retrieve()
				.toEntity(AcmeCredentialStatus.class);
		assertThat(change.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(change.getBody()).isNotNull();

		assertThat(basicClient("self.service", "initial-pass-123").get()
				.uri("/api/auth/session")
				.retrieve()
				.onStatus(s -> s.value() == 401, (request, response) -> {
				})
				.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(basicClient("self.service", "changed-pass-456").get()
				.uri("/api/auth/session")
				.retrieve()
				.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void adminResetPasswordAllowsTheUserToAuthenticateAndStoresOnlyHashes() {
		RestClient admin = authenticatedClient();
		User user = admin.post()
				.uri("/api/users")
				.body(new CreateUserRequest("reset.me", "reset@example.com", "Reset", "Me", null))
				.retrieve()
				.body(User.class);

		admin.put()
				.uri("/api/acme/users/{userId}/password", user.id())
				.body(new ResetPasswordRequest("reset-pass-123", false))
				.retrieve()
				.toBodilessEntity();

		assertThat(basicClient("reset.me", "reset-pass-123").get()
				.uri("/api/auth/session")
				.retrieve()
				.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.OK);

		String passwordHash = jdbcTemplate.queryForObject(
				"select password_hash from acme_user_credential where user_id = ?",
				String.class,
				user.id());
		assertThat(passwordHash).startsWith("{bcrypt}").doesNotContain("reset-pass-123");
	}

	@Test
	void failedLoginsLockTheAccountAndAdminCanUnlock() {
		RestClient admin = authenticatedClient();
		User user = admin.post()
				.uri("/api/users")
				.body(new CreateUserRequest("lockout.user", "lockout@example.com", "Lockout", "User", null))
				.retrieve()
				.body(User.class);
		admin.put()
				.uri("/api/acme/users/{userId}/password", user.id())
				.body(new ResetPasswordRequest("lockout-pass-123", false))
				.retrieve()
				.toBodilessEntity();

		RestClient wrong = basicClient("lockout.user", "wrong-password");
		for (int i = 0; i < 5; i++) {
			assertThat(wrong.get().uri("/api/auth/session").retrieve()
					.onStatus(s -> s.value() == 401, (request, response) -> {
					})
					.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		}

		RestClient correct = basicClient("lockout.user", "lockout-pass-123");
		assertThat(correct.get().uri("/api/auth/session").retrieve()
				.onStatus(s -> s.value() == 401, (request, response) -> {
				})
				.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		admin.post()
				.uri("/api/acme/users/{userId}/unlock", user.id())
				.retrieve()
				.toBodilessEntity();

		assertThat(correct.get().uri("/api/auth/session").retrieve()
				.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void resetWithRequireChangePersistsFlagAndEnforcesItOnOrdinaryApis() {
		RestClient admin = authenticatedClient();
		User user = admin.post()
				.uri("/api/users")
				.body(new CreateUserRequest("forced.change", "forced@example.com", "Forced", "Change", null))
				.retrieve()
				.body(User.class);

		admin.put()
				.uri("/api/acme/users/{userId}/password", user.id())
				.body(new ResetPasswordRequest("temp-pass-123", true))
				.retrieve()
				.toBodilessEntity();

		AcmeCredentialStatus status = admin.get()
				.uri("/api/acme/users/{userId}/credential", user.id())
				.retrieve()
				.body(AcmeCredentialStatus.class);
		assertThat(status.passwordChangeRequired()).isTrue();

		RestClient userClient = basicClient("forced.change", "temp-pass-123");
		ResponseEntity<String> session = userClient.get()
				.uri("/api/auth/session")
				.retrieve()
				.toEntity(String.class);
		assertThat(session.getBody()).contains("\"passwordChangeRequired\":true");

		assertThat(userClient.get().uri("/api/customers").retrieve()
				.onStatus(s -> s.value() == 403, (request, response) -> {
				})
				.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<AcmeCredentialStatus> changed = userClient.post()
				.uri("/api/me/password")
				.body(new ChangePasswordRequest("temp-pass-123", "final-pass-456"))
				.retrieve()
				.toEntity(AcmeCredentialStatus.class);
		assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(changed.getBody()).isNotNull();
		assertThat(changed.getBody().passwordChangeRequired()).isFalse();

		assertThat(basicClient("forced.change", "final-pass-456").get()
				.uri("/api/customers")
				.retrieve()
				.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(admin.get()
				.uri("/api/acme/users/{userId}/credential", user.id())
				.retrieve()
				.body(AcmeCredentialStatus.class).passwordChangeRequired()).isFalse();
	}

	@Test
	void resetWithoutRequireChangeAllowsImmediateAccess() {
		RestClient admin = authenticatedClient();
		User user = admin.post()
				.uri("/api/users")
				.body(new CreateUserRequest("optional.change", "optional@example.com", "Optional", "Change", null))
				.retrieve()
				.body(User.class);

		admin.put()
				.uri("/api/acme/users/{userId}/password", user.id())
				.body(new ResetPasswordRequest("direct-pass-123", false))
				.retrieve()
				.toBodilessEntity();

		AcmeCredentialStatus status = admin.get()
				.uri("/api/acme/users/{userId}/credential", user.id())
				.retrieve()
				.body(AcmeCredentialStatus.class);
		assertThat(status.passwordChangeRequired()).isFalse();

		assertThat(basicClient("optional.change", "direct-pass-123").get()
				.uri("/api/customers")
				.retrieve()
				.toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void passwordPolicyRejectsOutOfRangePasswords() {
		RestClient admin = authenticatedClient();
		User user = admin.post()
				.uri("/api/users")
				.body(new CreateUserRequest("policy.user", "policy@example.com", "Policy", "User", null))
				.retrieve()
				.body(User.class);

		ResponseEntity<AcmeCredentialStatus> tooShort = admin.put()
				.uri("/api/acme/users/{userId}/password", user.id())
				.body(new ResetPasswordRequest("short", false))
				.retrieve()
				.onStatus(s -> s.value() == 400, (request, response) -> {
				})
				.toEntity(AcmeCredentialStatus.class);
		assertThat(tooShort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		ResponseEntity<AcmeCredentialStatus> tooLong = admin.put()
				.uri("/api/acme/users/{userId}/password", user.id())
				.body(new ResetPasswordRequest("a".repeat(73), false))
				.retrieve()
				.onStatus(s -> s.value() == 400, (request, response) -> {
				})
				.toEntity(AcmeCredentialStatus.class);
		assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		AcmeCredentialStatus status = admin.get()
				.uri("/api/acme/users/{userId}/credential", user.id())
				.retrieve()
				.body(AcmeCredentialStatus.class);
		assertThat(status.hasCredential()).isFalse();
	}

	private RestClient authenticatedClient() {
		return RestClient.builder()
				.baseUrl("http://localhost:" + serverPort)
				.defaultHeaders(headers -> headers.setBasicAuth("test-user", "test-password"))
				.build();
	}

	private RestClient basicClient(String username, String password) {
		return RestClient.builder()
				.baseUrl("http://localhost:" + serverPort)
				.defaultHeaders(headers -> headers.setBasicAuth(username, password))
				.build();
	}

	private record CreateCustomerRequest(String name) {
	}

	private record CreateUserRequest(
			String username,
			String email,
			String givenName,
			String familyName,
			String phone) {
	}

	private record CreateRoleRequest(String name, String description) {
	}

	private record CreateGroupRequest(String name, String description) {
	}

	private record UpdateMyProfileRequest(
			String email,
			String givenName,
			String familyName,
			String phone) {
	}

	private record SaveProfileRequest(String serviceRegion, String accountNotes) {
	}

	private record CreateJobRequest(
			UUID customerId,
			String title,
			String description,
			String priority,
			String scheduledDate) {
	}

	private record UpdateJobRequest(
			String title,
			String description,
			String priority,
			String scheduledDate) {
	}

	private record ChangePasswordRequest(String currentPassword, String newPassword) {
	}

	private record ResetPasswordRequest(String newPassword, boolean requireChange) {
	}

	private static MultiValueMap<String, Object> multipart(
			String filename,
			byte[] bytes,
			String ownerType,
			String ownerId) {
		LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		});
		if (ownerType != null) {
			body.add("ownerType", ownerType);
		}
		if (ownerId != null) {
			body.add("ownerId", ownerId);
		}
		return body;
	}

	private record RenameDocumentRequest(String displayName) {
	}
}
