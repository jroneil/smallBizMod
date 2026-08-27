package com.oneilsoftware.sbp.module.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.contact.api.Contact;
import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.api.ContactStatus;
import com.oneilsoftware.sbp.module.contact.api.CreateContact;
import com.oneilsoftware.sbp.module.contact.api.UnknownCustomerException;
import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
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
	private CustomerOperations customerOperations;

	@Autowired
	private ContactOperations contactOperations;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@LocalServerPort
	private int serverPort;

	@Test
	void contactOwnsItsMigrationAndTableWithoutACrossModuleForeignKey() {
		List<String> migrationScripts = jdbcTemplate.queryForList(
				"select script from flyway_schema_history order by installed_rank",
				String.class);
		Integer crossModuleForeignKeys = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.table_constraints
				where table_schema = 'public'
				  and table_name = 'sbp_contact'
				  and constraint_type = 'FOREIGN KEY'
				""", Integer.class);

		assertThat(migrationScripts).containsExactly(
				"V1__create_sbp_customer.sql",
				"V3__create_sbp_contact.sql");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_contact')",
				String.class)).isEqualTo("sbp_contact");
		assertThat(crossModuleForeignKeys).isZero();
	}

	@Test
	void contactPersistsReloadsAndListsByCustomer() {
		Customer customer = customerOperations.create(new CreateCustomer("Northwind Repair"));
		Customer anotherCustomer = customerOperations.create(new CreateCustomer("Contoso Repair"));

		Contact created = contactOperations.create(new CreateContact(
				customer.id(),
				"  Avery Morgan  ",
				" avery@example.com ",
				" 555-0102 ",
				" Operations Manager "));
		contactOperations.create(new CreateContact(
				anotherCustomer.id(),
				"Jordan Lee",
				null,
				null,
				null));

		String persistedName = jdbcTemplate.queryForObject(
				"select full_name from sbp_contact where id = ?",
				String.class,
				created.id());

		assertThat(created.status()).isEqualTo(ContactStatus.ACTIVE);
		assertThat(created.fullName()).isEqualTo("Avery Morgan");
		assertThat(persistedName).isEqualTo("Avery Morgan");
		assertThat(contactOperations.findById(created.id())).contains(created);
		assertThat(contactOperations.listByCustomer(customer.id())).containsExactly(created);
	}

	@Test
	void contactRejectsAnUnknownCustomerBeforePersistence() {
		UUID unknownCustomerId = UUID.randomUUID();
		Integer contactsBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_contact",
				Integer.class);

		assertThatThrownBy(() -> contactOperations.create(new CreateContact(
				unknownCustomerId,
				"Unknown Customer Contact",
				null,
				null,
				null)))
				.isInstanceOf(UnknownCustomerException.class)
				.hasMessageContaining(unknownCustomerId.toString());
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_contact",
				Integer.class)).isEqualTo(contactsBefore);
	}

	@Test
	void restApiCreatesGetsAndListsContactsForACustomer() {
		Customer customer = customerOperations.create(new CreateCustomer("Fabrikam Field Service"));
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		ResponseEntity<Contact> createResponse = client.post()
				.uri("/api/contacts")
				.body(new CreateRequest(
						customer.id(),
						"Morgan Reyes",
						"morgan@example.com",
						"555-0134",
						"Owner"))
				.retrieve()
				.toEntity(Contact.class);
		Contact created = createResponse.getBody();
		assertThat(created).isNotNull();

		Contact retrieved = client.get()
				.uri("/api/contacts/{contactId}", created.id())
				.retrieve()
				.body(Contact.class);
		Contact[] contacts = client.get()
				.uri("/api/customers/{customerId}/contacts", customer.id())
				.retrieve()
				.body(Contact[].class);

		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(retrieved).isEqualTo(created);
		assertThat(contacts).containsExactly(created);
	}

	private record CreateRequest(
			UUID customerId,
			String fullName,
			String email,
			String phone,
			String roleTitle) {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}
}
