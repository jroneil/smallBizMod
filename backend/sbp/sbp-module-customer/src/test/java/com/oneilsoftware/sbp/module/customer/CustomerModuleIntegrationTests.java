package com.oneilsoftware.sbp.module.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.customer.api.CustomerStatus;
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
		classes = CustomerModuleIntegrationTests.TestApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.jpa.hibernate.ddl-auto=validate")
class CustomerModuleIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private CustomerOperations customerOperations;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@LocalServerPort
	private int serverPort;

	@Test
	void customerMigrationCreatesOwnedTable() {
		String tableName = jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_customer')",
				String.class);
		Integer migrationCount = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where script = 'V1__create_sbp_customer.sql'",
				Integer.class);

		assertThat(tableName).isEqualTo("sbp_customer");
		assertThat(migrationCount).isEqualTo(1);
	}

	@Test
	void publicCapabilityCreatesPersistsRetrievesAndListsCustomer() {
		Customer created = customerOperations.create(new CreateCustomer("Northwind Services"));

		String persistedName = jdbcTemplate.queryForObject(
				"select name from sbp_customer where id = ?",
				String.class,
				created.id());

		assertThat(created.status()).isEqualTo(CustomerStatus.ACTIVE);
		assertThat(persistedName).isEqualTo("Northwind Services");
		assertThat(customerOperations.findById(created.id())).contains(created);
		assertThat(customerOperations.list()).contains(created);
	}

	@Test
	void restApiCreatesRetrievesAndListsCustomer() {
		RestClient client = RestClient.create("http://localhost:" + serverPort);
		ResponseEntity<Customer> createResponse = client.post()
				.uri("/api/customers")
				.body(new CreateRequest("Contoso Repair"))
				.retrieve()
				.toEntity(Customer.class);

		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Customer created = createResponse.getBody();
		assertThat(created).isNotNull();

		Customer retrieved = client.get()
				.uri("/api/customers/{customerId}", created.id())
				.retrieve()
				.body(Customer.class);
		Customer[] customers = client.get()
				.uri("/api/customers")
				.retrieve()
				.body(Customer[].class);

		assertThat(retrieved).isEqualTo(created);
		assertThat(customers).contains(created);
	}

	private record CreateRequest(String name) {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}
}
