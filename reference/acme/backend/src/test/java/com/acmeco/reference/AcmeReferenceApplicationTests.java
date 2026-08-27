package com.acmeco.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.acmeco.reference.customer.api.AcmeCustomerView;
import com.acmeco.reference.customer.api.SaveAcmeCustomerProfile;
import com.acmeco.reference.customer.internal.application.AcmeCustomerProfileService;
import com.oneilsoftware.sbp.module.contact.api.Contact;
import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.api.CreateContact;
import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
				"acme.security.password=test-password"
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
				"V3__create_sbp_contact.sql");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.acme_customer_profile')",
				String.class)).isEqualTo("acme_customer_profile");
		assertThat(customerColumns).containsExactly("id", "name", "status");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_contact')",
				String.class)).isEqualTo("sbp_contact");
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
		assertThat(response.getBody()).isEqualTo("{\"username\":\"test-user\"}");
	}

	private RestClient authenticatedClient() {
		return RestClient.builder()
				.baseUrl("http://localhost:" + serverPort)
				.defaultHeaders(headers -> headers.setBasicAuth("test-user", "test-password"))
				.build();
	}

	private record CreateCustomerRequest(String name) {
	}

	private record SaveProfileRequest(String serviceRegion, String accountNotes) {
	}
}
