package com.oneilsoftware.sbp.module.contact;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.oneilsoftware.sbp.module.contact.api.Contact;
import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.api.ContactStatus;
import com.oneilsoftware.sbp.module.contact.api.CreateContact;
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
		classes = ContactModuleIntegrationTests.TestApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.jpa.hibernate.ddl-auto=validate")
class ContactModuleIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private ContactOperations contactOperations;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@LocalServerPort
	private int serverPort;

	@Test
	void contactMigrationCreatesOwnedTableWithoutCustomerDependency() {
		String tableName = jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_contact')",
				String.class);
		Integer migrationCount = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where script = 'V3__create_sbp_contact.sql'",
				Integer.class);

		assertThat(tableName).isEqualTo("sbp_contact");
		assertThat(migrationCount).isEqualTo(1);
	}

	@Test
	void publicCapabilityCreatesPersistsRetrievesAndListsContact() {
		UUID customerId = UUID.randomUUID();
		Contact created = contactOperations.create(new CreateContact(
				customerId,
				"Dana Whitfield",
				"Office Manager",
				"dana@example.com",
				"555-0100"));

		String persistedName = jdbcTemplate.queryForObject(
				"select full_name from sbp_contact where id = ?",
				String.class,
				created.id());
		String persistedCustomerId = jdbcTemplate.queryForObject(
				"select customer_id::text from sbp_contact where id = ?",
				String.class,
				created.id());

		assertThat(created.status()).isEqualTo(ContactStatus.ACTIVE);
		assertThat(persistedName).isEqualTo("Dana Whitfield");
		assertThat(persistedCustomerId).isEqualTo(customerId.toString());
		assertThat(contactOperations.findById(created.id())).contains(created);
		assertThat(contactOperations.listByCustomer(customerId)).containsExactly(created);
		assertThat(contactOperations.list()).contains(created);
	}

	@Test
	void listByCustomerDoesNotLeakOtherCustomersContacts() {
		UUID acmeCustomer = UUID.randomUUID();
		UUID otherCustomer = UUID.randomUUID();

		Contact acmeContact = contactOperations.create(new CreateContact(
				acmeCustomer,
				"Acme Contact",
				null,
				null,
				null));
		contactOperations.create(new CreateContact(
				otherCustomer,
				"Other Contact",
				null,
				null,
				null));

		assertThat(contactOperations.listByCustomer(acmeCustomer))
				.containsExactly(acmeContact);
	}

	@Test
	void restApiCreatesRetrievesAndListsContact() {
		RestClient client = RestClient.create("http://localhost:" + serverPort);
		UUID customerId = UUID.randomUUID();

		ResponseEntity<Contact> createResponse = client.post()
				.uri("/api/contacts")
				.body(new CreateRequest(customerId, "Jamie Reyes", "Technician", "jamie@example.com", "555-0199"))
				.retrieve()
				.toEntity(Contact.class);

		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Contact created = createResponse.getBody();
		assertThat(created).isNotNull();

		Contact retrieved = client.get()
				.uri("/api/contacts/{contactId}", created.id())
				.retrieve()
				.body(Contact.class);
		Contact[] contacts = client.get()
				.uri("/api/contacts?customerId={customerId}", customerId)
				.retrieve()
				.body(Contact[].class);

		assertThat(retrieved).isEqualTo(created);
		assertThat(contacts).containsExactly(created);
	}

	private record CreateRequest(
			UUID customerId,
			String fullName,
			String role,
			String email,
			String phone) {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}
}
