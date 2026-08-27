package com.oneilsoftware.sbp.module.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.oneilsoftware.sbp.framework.storage.BlobStorage;
import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.document.api.CreateDocument;
import com.oneilsoftware.sbp.module.document.api.Document;
import com.oneilsoftware.sbp.module.document.api.DocumentOperations;
import com.oneilsoftware.sbp.module.document.api.DocumentOwnerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves the FR-11 storage seam (ADR-DOC-002): when a customer deployment supplies its own
 * {@link BlobStorage} bean, the module's database-backed default backs off and the Document
 * capability works unchanged against the customer's implementation — content never touches the
 * module's {@code sbp_document_content} table.
 */
@Testcontainers
@SpringBootTest(
		classes = DocumentBlobStorageOverrideTests.TestApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.jpa.hibernate.ddl-auto=validate")
class DocumentBlobStorageOverrideTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private CustomerOperations customerOperations;

	@Autowired
	private DocumentOperations documentOperations;

	@Autowired
	private BlobStorage blobStorage;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void customerSuppliedBlobStorageReplacesTheModuleDefault() {
		assertThat(blobStorage).isInstanceOf(InMemoryBlobStorage.class);

		Customer customer = customerOperations.create(new CreateCustomer("Override Co"));
		byte[] bytes = "OVERRIDE-CONTENT".getBytes();
		Document document = documentOperations.create(new CreateDocument(
				DocumentOwnerType.CUSTOMER, customer.id(), "x.pdf", "application/pdf", bytes));

		assertThat(documentOperations.download(document.id()).content()).isEqualTo(bytes);

		// Content lives only in the customer's implementation, never in the module's database table.
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_document_content where storage_key = ?",
				Integer.class,
				document.id())).isZero();
		assertThat(((InMemoryBlobStorage) blobStorage).stored.get(document.id().toString())).isEqualTo(bytes);

		documentOperations.delete(document.id());
		assertThat(((InMemoryBlobStorage) blobStorage).stored).isEmpty();
	}

	static class InMemoryBlobStorage implements BlobStorage {

		final Map<String, byte[]> stored = new ConcurrentHashMap<>();

		@Override
		public void store(String key, byte[] content, String contentType) {
			stored.put(key, content);
		}

		@Override
		public Optional<byte[]> load(String key) {
			return Optional.ofNullable(stored.get(key));
		}

		@Override
		public void delete(String key) {
			stored.remove(key);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {

		@Bean
		BlobStorage blobStorage() {
			return new InMemoryBlobStorage();
		}
	}
}
