package com.oneilsoftware.sbp.module.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.framework.storage.BlobStorage;
import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.document.api.CreateDocument;
import com.oneilsoftware.sbp.module.document.api.Document;
import com.oneilsoftware.sbp.module.document.api.DocumentAuditEvent;
import com.oneilsoftware.sbp.module.document.api.DocumentContent;
import com.oneilsoftware.sbp.module.document.api.DocumentFilter;
import com.oneilsoftware.sbp.module.document.api.DocumentOperations;
import com.oneilsoftware.sbp.module.document.api.DocumentOwnerType;
import com.oneilsoftware.sbp.module.document.api.DocumentPage;
import com.oneilsoftware.sbp.module.document.api.DocumentSizeLimitExceededException;
import com.oneilsoftware.sbp.module.document.api.ReplaceDocumentContent;
import com.oneilsoftware.sbp.module.document.api.UnknownDocumentException;
import com.oneilsoftware.sbp.module.document.api.UnknownDocumentOwnerException;
import com.oneilsoftware.sbp.module.document.api.UpdateDocumentMetadata;
import com.oneilsoftware.sbp.module.document.internal.DocumentProperties;
import com.oneilsoftware.sbp.module.job.api.CreateJob;
import com.oneilsoftware.sbp.module.job.api.Job;
import com.oneilsoftware.sbp.module.job.api.JobOperations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
		classes = DocumentModuleIntegrationTests.TestApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.jpa.hibernate.ddl-auto=validate")
class DocumentModuleIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private CustomerOperations customerOperations;

	@Autowired
	private JobOperations jobOperations;

	@Autowired
	private DocumentOperations documentOperations;

	@Autowired
	private BlobStorage blobStorage;

	@Autowired
	private DocumentProperties documentProperties;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@LocalServerPort
	private int serverPort;

	@Test
	void documentOwnsItsMigrationsAndTablesWithoutACrossModuleForeignKey() {
		List<String> migrationScripts = jdbcTemplate.queryForList(
				"select script from flyway_schema_history order by installed_rank",
				String.class);
		Integer crossModuleForeignKeys = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.table_constraints
				where table_schema = 'public'
				  and table_name = 'sbp_document'
				  and constraint_type = 'FOREIGN KEY'
				""", Integer.class);

		assertThat(migrationScripts).containsExactly(
				"V1__create_sbp_customer.sql",
				"V14__create_sbp_job.sql",
				"V15__create_sbp_job_audit_event.sql",
				"V16__create_sbp_document.sql",
				"V17__create_sbp_document_content.sql",
				"V18__create_sbp_document_audit_event.sql");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_document')",
				String.class)).isEqualTo("sbp_document");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_document_content')",
				String.class)).isEqualTo("sbp_document_content");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_document_audit_event')",
				String.class)).isEqualTo("sbp_document_audit_event");
		assertThat(crossModuleForeignKeys).isZero();

		// Metadata and content are separate concerns (ADR-DOC-002): the metadata row never holds
		// content bytes, and the audit table is value-free — no column exists that could store
		// file content or content hashes (DOC-FR-09).
		List<String> documentColumns = jdbcTemplate.queryForList("""
				select column_name
				from information_schema.columns
				where table_schema = 'public' and table_name = 'sbp_document'
				order by ordinal_position
				""", String.class);
		assertThat(documentColumns).containsExactly(
				"id", "customer_id", "job_id", "display_name", "content_type", "size_bytes",
				"created_at", "updated_at");
		List<String> auditColumns = jdbcTemplate.queryForList("""
				select column_name
				from information_schema.columns
				where table_schema = 'public' and table_name = 'sbp_document_audit_event'
				order by ordinal_position
				""", String.class);
		assertThat(auditColumns).containsExactly(
				"id", "actor", "subject_type", "subject_id", "action", "changed_fields", "detail",
				"occurred_at");
	}

	@Test
	void documentCreatesPersistsReloadsAndListsByOwner() {
		Customer customer = customerOperations.create(new CreateCustomer("Northwind Repair"));
		Job job = jobOperations.create(new CreateJob(customer.id(), "Replace water heater", null, null, null));

		Document customerDoc = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customer.id(), "contract.pdf", "application/pdf", "PDF-BYTES".getBytes()));
		Document jobDoc = documentOperations.create(new CreateDocument(
				DocumentOwnerType.JOB, job.id(), "site-photo.jpg", "image/jpeg", "JPEG-BYTES".getBytes()));

		String persistedName = jdbcTemplate.queryForObject(
				"select display_name from sbp_document where id = ?",
				String.class,
				customerDoc.id());

		assertThat(customerDoc.ownerType()).isEqualTo(DocumentOwnerType.CUSTOMER);
		assertThat(customerDoc.ownerId()).isEqualTo(customer.id());
		assertThat(jobDoc.ownerType()).isEqualTo(DocumentOwnerType.JOB);
		assertThat(jobDoc.ownerId()).isEqualTo(job.id());
		assertThat(persistedName).isEqualTo("contract.pdf");
		assertThat(customerDoc.sizeBytes()).isEqualTo("PDF-BYTES".getBytes().length);
		assertThat(documentOperations.findById(customerDoc.id())).contains(customerDoc);

		assertThat(documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customer.id(), 0, 20)).items())
				.containsExactly(customerDoc);
		assertThat(documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.JOB, job.id(), 0, 20)).items())
				.containsExactly(jobDoc);
	}

	@Test
	void documentStoresOnlyTheBaseNameOfUploadedPaths() {
		Customer customer = customerOperations.create(new CreateCustomer("Path Co"));

		Document document = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER,
				customer.id(),
				"../uploads/2026/lease agreement.pdf",
				"application/pdf",
				"PDF-BYTES".getBytes()));

		assertThat(document.displayName()).isEqualTo("lease agreement.pdf");
	}

	@Test
	void documentRejectsUnknownOwnersBeforePersistence() {
		UUID unknownCustomerId = UUID.randomUUID();
		UUID unknownJobId = UUID.randomUUID();
		Integer documentsBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_document", Integer.class);
		Integer contentBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_content", Integer.class);

		assertThatThrownBy(() -> documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, unknownCustomerId, "x.pdf", null, "BYTES".getBytes())))
				.isInstanceOf(UnknownDocumentOwnerException.class);
		assertThatThrownBy(() -> documentOperations.create(new CreateDocument(
				DocumentOwnerType.JOB, unknownJobId, "x.pdf", null, "BYTES".getBytes())))
				.isInstanceOf(UnknownDocumentOwnerException.class);

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_document", Integer.class)).isEqualTo(documentsBefore);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_content", Integer.class)).isEqualTo(contentBefore);
	}

	@Test
	void documentRejectsOversizedContentBeforeStoringAnything() {
		Customer customer = customerOperations.create(new CreateCustomer("Big File Co"));
		byte[] oversized = new byte[(int) documentProperties.maxSizeBytes() + 1];
		Integer documentsBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_document", Integer.class);
		Integer contentBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_content", Integer.class);

		assertThatThrownBy(() -> documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customer.id(), "big.pdf", null, oversized)))
				.isInstanceOf(DocumentSizeLimitExceededException.class);

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_document", Integer.class)).isEqualTo(documentsBefore);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_content", Integer.class)).isEqualTo(contentBefore);
	}

	@Test
	void documentDownloadReturnsIdenticalBytesWithContentTypeAndDisplayName() {
		Customer customer = customerOperations.create(new CreateCustomer("Download Co"));
		byte[] bytes = "THE-EXACT-CONTENT".getBytes();
		Document document = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customer.id(), "warranty.pdf", "application/pdf", bytes));

		DocumentContent downloaded = documentOperations.download(document.id());

		assertThat(downloaded.content()).isEqualTo(bytes);
		assertThat(downloaded.contentType()).isEqualTo("application/pdf");
		assertThat(downloaded.displayName()).isEqualTo("warranty.pdf");
	}

	@Test
	void documentRenameUpdatesDisplayNameAndAuditsIt() {
		Customer customer = customerOperations.create(new CreateCustomer("Rename Co"));
		Document document = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customer.id(), "typo.pdf", "application/pdf", "BYTES".getBytes()));

		Document renamed = documentOperations.updateDisplayName(
				document.id(), new UpdateDocumentMetadata("corrected.pdf"));

		assertThat(renamed.displayName()).isEqualTo("corrected.pdf");
		assertThat(documentOperations.findById(document.id())).contains(renamed);
		List<String> actions = auditActionsFor(document.id());
		assertThat(actions).containsExactly("DOCUMENT_CREATED", "DOCUMENT_METADATA_UPDATED");
		String changedFields = jdbcTemplate.queryForObject(
				"select changed_fields from sbp_document_audit_event where subject_id = ? and action = 'DOCUMENT_METADATA_UPDATED'",
				String.class,
				document.id().toString());
		assertThat(changedFields).isEqualTo("displayName");
	}

	@Test
	void documentContentReplacementUpdatesContentSizeAndType() {
		Customer customer = customerOperations.create(new CreateCustomer("Replace Co"));
		Document document = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER,
				customer.id(),
				"draft.pdf",
				"application/pdf",
				"OLD-CONTENT".getBytes()));
		byte[] replacement = "NEW-CONTENT-LONGER".getBytes();

		Document replaced = documentOperations.replaceContent(
				document.id(),
				new ReplaceDocumentContent("application/pdf", replacement));

		assertThat(replaced.sizeBytes()).isEqualTo(replacement.length);
		assertThat(documentOperations.download(document.id()).content()).isEqualTo(replacement);
		assertThat(documentOperations.findById(document.id()).orElseThrow().displayName()).isEqualTo("draft.pdf");
		assertThat(auditActionsFor(document.id())).containsExactly(
				"DOCUMENT_CREATED", "DOCUMENT_CONTENT_REPLACED");
	}

	@Test
	void documentDeleteRemovesMetadataAndContentButKeepsAuditHistory() {
		Customer customer = customerOperations.create(new CreateCustomer("Delete Co"));
		Document document = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customer.id(), "obsolete.pdf", "application/pdf", "BYTES".getBytes()));

		documentOperations.delete(document.id());

		assertThat(documentOperations.findById(document.id())).isEmpty();
		assertThat(documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customer.id(), 0, 20)).items()).isEmpty();
		assertThatThrownBy(() -> documentOperations.download(document.id()))
				.isInstanceOf(UnknownDocumentException.class);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_content where storage_key = ?",
				Integer.class,
				document.id())).isZero();

		// The audit history remains readable after deletion (DOC-FR-06).
		assertThat(auditActionsFor(document.id())).containsExactlyInAnyOrder(
				"DOCUMENT_DELETED", "DOCUMENT_CREATED");
		List<DocumentAuditEvent> events = documentOperations.listAuditEvents(document.id(), null);
		assertThat(events).extracting(DocumentAuditEvent::action)
				.containsExactlyInAnyOrder("DOCUMENT_DELETED", "DOCUMENT_CREATED");
		assertThat(events.get(0).occurredAt()).isAfterOrEqualTo(events.get(events.size() - 1).occurredAt());
		assertThat(documentOperations.listAuditEvents(document.id(), "DOCUMENT_DELETED"))
				.extracting(DocumentAuditEvent::action)
				.containsExactly("DOCUMENT_DELETED");
	}

	@Test
	void documentSearchFiltersAndPaginates() {
		Customer customerA = customerOperations.create(new CreateCustomer("A Search Co"));
		Customer customerB = customerOperations.create(new CreateCustomer("B Search Co"));

		Document lease = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customerA.id(), "lease-agreement.pdf", "application/pdf", "A".getBytes()));
		Document invoice = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customerA.id(), "INVOICE-2026.pdf", "application/pdf", "B".getBytes()));
		Document other = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customerB.id(), "lease-agreement-copy.pdf", "application/pdf", "C".getBytes()));

		// Text query matches the display name case-insensitively. Assertions are scoped to an owner
		// because the shared Testcontainers database accumulates documents from other test methods;
		// scoping keeps them deterministic regardless of test order.
		assertThat(documentOperations.search(new DocumentFilter(
				"lease", DocumentOwnerType.CUSTOMER, customerA.id(), 0, 20)).items())
				.containsExactly(lease);
		assertThat(documentOperations.search(new DocumentFilter(
				"INVOICE", DocumentOwnerType.CUSTOMER, customerA.id(), 0, 20)).items())
				.containsExactly(invoice);

		// Owner filtering narrows to one Customer's Documents.
		assertThat(documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customerA.id(), 0, 20)).items())
				.containsExactly(invoice, lease);
		assertThat(documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customerB.id(), 0, 20)).items())
				.containsExactly(other);

		// Pagination with deterministic newest-first ordering, scoped to one owner.
		DocumentPage page = documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customerA.id(), 0, 1));
		assertThat(page.items()).containsExactly(invoice);
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(1);

		DocumentPage secondPage = documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customerA.id(), 1, 1));
		assertThat(secondPage.items()).containsExactly(lease);
	}

	@Test
	void documentSearchPaginatesAtVolumeAgainstThePerformanceExpectation() {
		Customer customer = customerOperations.create(new CreateCustomer("Volume Co"));
		int total = 1050;
		for (int i = 0; i < total; i++) {
			documentOperations.create(new CreateDocument(
					DocumentOwnerType.CUSTOMER,
					customer.id(),
					"volume-doc-" + i + ".pdf",
					"application/pdf",
					("content-" + i).getBytes()));
		}

		DocumentPage firstPage = documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customer.id(), 0, 20));
		assertThat(firstPage.totalElements()).isEqualTo(total);
		assertThat(firstPage.totalPages()).isEqualTo(53);
		assertThat(firstPage.items()).hasSize(20);
		assertThat(firstPage.items().get(0).displayName()).isEqualTo("volume-doc-" + (total - 1) + ".pdf");
		assertThat(firstPage.items().get(19).displayName()).isEqualTo("volume-doc-" + (total - 20) + ".pdf");

		DocumentPage lastPage = documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customer.id(), 52, 20));
		assertThat(lastPage.items()).hasSize(10);

		DocumentPage beyond = documentOperations.search(new DocumentFilter(
				null, DocumentOwnerType.CUSTOMER, customer.id(), 99, 20));
		assertThat(beyond.items()).isEmpty();
		assertThat(beyond.totalElements()).isEqualTo(total);

		// The text query still narrows at volume.
		DocumentPage matched = documentOperations.search(new DocumentFilter(
				"volume-doc-500", DocumentOwnerType.CUSTOMER, customer.id(), 0, 20));
		assertThat(matched.totalElements()).isEqualTo(1);
		assertThat(matched.items().get(0).displayName()).isEqualTo("volume-doc-500.pdf");
	}

	@Test
	void documentAuditRecordsFieldNamesOnlyWithTheModuleDefaultActor() {
		Customer customer = customerOperations.create(new CreateCustomer("Audit Co"));
		Document document = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customer.id(), "audited.pdf", "application/pdf", "BYTES".getBytes()));
		documentOperations.updateDisplayName(document.id(), new UpdateDocumentMetadata("renamed.pdf"));

		List<String> actors = jdbcTemplate.queryForList(
				"select distinct actor from sbp_document_audit_event where subject_id = ?",
				String.class,
				document.id().toString());
		// The module default actor is "system" in this composition (no customer resolver supplied).
		assertThat(actors).containsExactly("system");

		String createdFields = jdbcTemplate.queryForObject(
				"select changed_fields from sbp_document_audit_event where subject_id = ? and action = 'DOCUMENT_CREATED'",
				String.class,
				document.id().toString());
		assertThat(createdFields).contains("ownerType").contains("displayName").contains("sizeBytes");

		// Failed operations write no audit events.
		Integer eventsBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_audit_event", Integer.class);
		assertThatThrownBy(() -> documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, UUID.randomUUID(), "x.pdf", null, "BYTES".getBytes())))
				.isInstanceOf(UnknownDocumentOwnerException.class);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_audit_event", Integer.class)).isEqualTo(eventsBefore);
	}

	@Test
	void databaseBlobStorageRoundTripsThroughTheDatabase() {
		String key = UUID.randomUUID().toString();
		byte[] content = "STORAGE-CONTENT".getBytes();

		blobStorage.store(key, content, "text/plain");
		assertThat(blobStorage.load(key)).contains(content);
		assertThat(jdbcTemplate.queryForObject(
				"select content from sbp_document_content where storage_key = ?",
				byte[].class,
				UUID.fromString(key))).isEqualTo(content);

		blobStorage.delete(key);
		assertThat(blobStorage.load(key)).isEmpty();
		// Deleting a missing key is a no-op.
		blobStorage.delete(key);
	}

	@Test
	void restApiUploadsListsDownloadsRenamesReplacesAndDeletes() {
		Customer customer = customerOperations.create(new CreateCustomer("Rest Api Co"));
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		ResponseEntity<Document> createResponse = client.post()
				.uri("/api/documents")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipart("contract.pdf", "application/pdf", "PDF-BYTES".getBytes(),
						"CUSTOMER", customer.id().toString(), null))
				.retrieve()
				.toEntity(Document.class);
		Document created = createResponse.getBody();
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created).isNotNull();
		assertThat(created.displayName()).isEqualTo("contract.pdf");

		DocumentPage page = client.get()
				.uri("/api/documents?ownerType=CUSTOMER&ownerId={ownerId}&page=0&size=10", customer.id())
				.retrieve()
				.body(DocumentPage.class);
		assertThat(page).isNotNull();
		assertThat(page.items()).containsExactly(created);

		ResponseEntity<byte[]> download = client.get()
				.uri("/api/documents/{documentId}/content", created.id())
				.retrieve()
				.toEntity(byte[].class);
		assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(download.getBody()).isEqualTo("PDF-BYTES".getBytes());
		assertThat(download.getHeaders().getFirst("Content-Disposition")).contains("attachment").contains("contract.pdf");

		Document renamed = client.put()
				.uri("/api/documents/{documentId}", created.id())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new RenameRequest("final-contract.pdf"))
				.retrieve()
				.body(Document.class);
		assertThat(renamed.displayName()).isEqualTo("final-contract.pdf");

		Document replaced = client.put()
				.uri("/api/documents/{documentId}/content", created.id())
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipart("final-contract.pdf", "application/pdf", "NEW-PDF-BYTES".getBytes(),
						null, null, null))
				.retrieve()
				.body(Document.class);
		assertThat(replaced.sizeBytes()).isEqualTo("NEW-PDF-BYTES".getBytes().length);
		assertThat(client.get()
				.uri("/api/documents/{documentId}/content", created.id())
				.retrieve()
				.body(byte[].class)).isEqualTo("NEW-PDF-BYTES".getBytes());

		ResponseEntity<Void> deleted = client.delete()
				.uri("/api/documents/{documentId}", created.id())
				.retrieve()
				.toBodilessEntity();
		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	void restApiAcceptsFilesWithinTheModuleLimitThatExceedBootsDefaultMultipartCap() {
		// Spring Boot's default web-layer cap is 1 MB; the module's MultipartConfigElement (from
		// sbp.document.max-size-bytes, default 10 MB) must be the active cap (NFR-DOC-07,
		// ADR-DOC-002). A 2 MB upload therefore succeeds, and a >10 MB one is rejected with 413.
		Customer customer = customerOperations.create(new CreateCustomer("Web Cap Co"));
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		byte[] twoMegabytes = new byte[2 * 1024 * 1024];
		ResponseEntity<Document> withinLimit = client.post()
				.uri("/api/documents")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipart("medium.pdf", "application/pdf", twoMegabytes,
						"CUSTOMER", customer.id().toString(), null))
				.retrieve()
				.toEntity(Document.class);
		assertThat(withinLimit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(withinLimit.getBody().sizeBytes()).isEqualTo(twoMegabytes.length);
	}

	@Test
	void restApiRejectsUnknownOwnersOversizedFilesAndUnknownDocuments() {
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		ResponseEntity<Void> unknownOwner = client.post()
				.uri("/api/documents")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipart("x.pdf", "application/pdf", "BYTES".getBytes(),
						"CUSTOMER", UUID.randomUUID().toString(), null))
				.retrieve()
				.onStatus(s -> s.value() == 404, (request, response) -> {
				})
				.toBodilessEntity();
		assertThat(unknownOwner.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<Void> oversized = client.post()
				.uri("/api/documents")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipart("big.pdf", "application/pdf",
						new byte[(int) documentProperties.maxSizeBytes() + 1],
						"CUSTOMER", UUID.randomUUID().toString(), null))
				.retrieve()
				.onStatus(s -> s.value() == 413, (request, response) -> {
				})
				.toBodilessEntity();
		assertThat(oversized.getStatusCode().value()).isEqualTo(413);

		ResponseEntity<Void> unknownDocument = client.get()
				.uri("/api/documents/{documentId}", UUID.randomUUID())
				.retrieve()
				.onStatus(s -> s.value() == 404, (request, response) -> {
				})
				.toBodilessEntity();
		assertThat(unknownDocument.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void restApiExposesPerDocumentAuditEventsIncludingAfterDeletion() {
		Customer customer = customerOperations.create(new CreateCustomer("Rest Audit Co"));
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		Document created = client.post()
				.uri("/api/documents")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(multipart("audited.pdf", "application/pdf", "BYTES".getBytes(),
						"CUSTOMER", customer.id().toString(), null))
				.retrieve()
				.body(Document.class);
		assertThat(created).isNotNull();
		client.delete().uri("/api/documents/{documentId}", created.id()).retrieve().toBodilessEntity();

		DocumentAuditEvent[] events = client.get()
				.uri("/api/documents/{documentId}/audit-events", created.id())
				.retrieve()
				.body(DocumentAuditEvent[].class);
		assertThat(events).extracting(DocumentAuditEvent::action)
				.containsExactlyInAnyOrder("DOCUMENT_DELETED", "DOCUMENT_CREATED");
		assertThat(events[0].occurredAt()).isAfterOrEqualTo(events[events.length - 1].occurredAt());

		DocumentAuditEvent[] filtered = client.get()
				.uri("/api/documents/{documentId}/audit-events?action=DOCUMENT_DELETED", created.id())
				.retrieve()
				.body(DocumentAuditEvent[].class);
		assertThat(filtered).extracting(DocumentAuditEvent::action).containsExactly("DOCUMENT_DELETED");
	}

	private List<String> auditActionsFor(UUID documentId) {
		return jdbcTemplate.queryForList(
				"select action from sbp_document_audit_event where subject_id = ? order by occurred_at",
				String.class,
				documentId.toString());
	}

	private static org.springframework.util.MultiValueMap<String, Object> multipart(
			String filename,
			String contentType,
			byte[] bytes,
			String ownerType,
			String ownerId,
			String displayName) {
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
		if (displayName != null) {
			body.add("displayName", displayName);
		}
		return body;
	}

	private record RenameRequest(String displayName) {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}
}
