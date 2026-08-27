package com.oneilsoftware.sbp.module.usermaintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.AuditActions;
import com.oneilsoftware.sbp.module.usermaintenance.api.AuditFilter;
import com.oneilsoftware.sbp.module.usermaintenance.api.AuditOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.BulkCreateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.BulkUserImportResult;
import com.oneilsoftware.sbp.module.usermaintenance.api.CreateGroup;
import com.oneilsoftware.sbp.module.usermaintenance.api.CreateRole;
import com.oneilsoftware.sbp.module.usermaintenance.api.CreateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.Group;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.Permission;
import com.oneilsoftware.sbp.module.usermaintenance.api.PermissionOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateGroup;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateRole;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserAuditEvent;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserFilter;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.UsernameAlreadyExistsException;
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
		classes = UserMaintenanceModuleIntegrationTests.TestApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.jpa.hibernate.ddl-auto=validate",
				"sbp.usermaintenance.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
				"sbp.usermaintenance.search-key=ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA="
		})
class UserMaintenanceModuleIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private UserOperations userOperations;

	@Autowired
	private RoleOperations roleOperations;

	@Autowired
	private GroupOperations groupOperations;

	@Autowired
	private AuditOperations auditOperations;

	@Autowired
	private PermissionOperations permissionOperations;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@LocalServerPort
	private int serverPort;

	@Test
	void userOwnsItsMigrationAndTable() {
		List<String> migrationScripts = jdbcTemplate.queryForList(
				"select script from flyway_schema_history order by installed_rank",
				String.class);

		assertThat(migrationScripts).containsExactly(
				"V4__create_sbp_user.sql",
				"V5__create_sbp_role_and_user_role.sql",
				"V6__create_sbp_group_and_user_group.sql",
				"V7__create_sbp_user_audit_event.sql",
				"V8__widen_sbp_user_pii_columns.sql",
				"V9__add_sbp_user_search_tokens.sql",
				"V10__create_sbp_permission_and_role_permission.sql",
				"V13__create_sbp_group_role.sql");
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
				"select to_regclass('public.sbp_group_role')",
				String.class)).isEqualTo("sbp_group_role");
		assertThat(jdbcTemplate.queryForObject(
				"select to_regclass('public.sbp_user_audit_event')",
				String.class)).isEqualTo("sbp_user_audit_event");
	}

	@Test
	void userLifecycleCreatesSearchesUpdatesDeactivatesAndReactivates() {
		User created = userOperations.create(new CreateUser(
				"  avery.morgan  ",
				" avery@example.com ",
				" Avery ",
				" Morgan ",
				" 555-0102 "));

		assertThat(created.username()).isEqualTo("avery.morgan");
		assertThat(created.email()).isEqualTo("avery@example.com");
		assertThat(created.status()).isEqualTo(UserStatus.ACTIVE);
		assertThat(created.createdAt()).isNotNull();
		assertThat(created.updatedAt()).isNotNull();
		assertThat(userOperations.findByUsername("avery.morgan")).contains(created);

		User updated = userOperations.update(created.id(), new UpdateUser(
				"avery.updated@example.com",
				"Avery",
				"Morgan",
				"555-0199"));
		assertThat(updated.email()).isEqualTo("avery.updated@example.com");
		assertThat(updated.phone()).isEqualTo("555-0199");

		User deactivated = userOperations.deactivate(created.id());
		assertThat(deactivated.status()).isEqualTo(UserStatus.INACTIVE);

		User reactivated = userOperations.activate(created.id());
		assertThat(reactivated.status()).isEqualTo(UserStatus.ACTIVE);

		assertThat(userOperations.search(new UserFilter("avery.morgan", UserStatus.ACTIVE, null, null)))
				.containsExactly(reactivated);
		assertThat(userOperations.search(new UserFilter("avery.morgan", UserStatus.INACTIVE, null, null)))
				.isEmpty();
	}

	@Test
	void searchHandlesNullQueryAndStatusFilters() {
		User activeUser = userOperations.create(new CreateUser(
				"search.null.active",
				"nullactive@example.com",
				"Null",
				"Active",
				null));

		assertThat(userOperations.search(new UserFilter(null, null, null, null))).contains(activeUser);
		assertThat(userOperations.search(new UserFilter(null, UserStatus.ACTIVE, null, null))).contains(activeUser);
		assertThat(userOperations.search(new UserFilter(null, UserStatus.INACTIVE, null, null))).doesNotContain(activeUser);
	}

	@Test
	void duplicateUsernameIsRejectedBeforePersistence() {
		userOperations.create(new CreateUser(
				"jordan.lee",
				"jordan@example.com",
				"Jordan",
				"Lee",
				null));
		Integer usersBefore = jdbcTemplate.queryForObject(
				"select count(*) from sbp_user",
				Integer.class);

		assertThatThrownBy(() -> userOperations.create(new CreateUser(
				"jordan.lee",
				"other@example.com",
				"Jordan",
				"Lee",
				null)))
				.isInstanceOf(UsernameAlreadyExistsException.class);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from sbp_user",
				Integer.class)).isEqualTo(usersBefore);
	}

	@Test
	void restApiCreatesListsGetsUpdatesAndChangesLifecycle() {
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		ResponseEntity<User> createResponse = client.post()
				.uri("/api/users")
				.body(new CreateRequest(
						"morgan.reyes",
						"morgan@example.com",
						"Morgan",
						"Reyes",
						"555-0134"))
				.retrieve()
				.toEntity(User.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		User created = createResponse.getBody();
		assertThat(created).isNotNull();

		User retrieved = client.get()
				.uri("/api/users/{userId}", created.id())
				.retrieve()
				.body(User.class);
		assertThat(retrieved).isEqualTo(created);

		User[] users = client.get()
				.uri("/api/users?query=morgan.reyes")
				.retrieve()
				.body(User[].class);
		assertThat(users).hasSize(1);
		assertThat(users[0].username()).isEqualTo("morgan.reyes");
		assertThat(users[0].email()).isEqualTo("m***@example.com");
		assertThat(users[0].phone()).isEqualTo("***0134");

		User updated = client.put()
				.uri("/api/users/{userId}", created.id())
				.body(new UpdateRequest(
						"morgan.updated@example.com",
						"Morgan",
						"Reyes",
						"555-0199"))
				.retrieve()
				.body(User.class);
		assertThat(updated.email()).isEqualTo("morgan.updated@example.com");

		User deactivated = client.post()
				.uri("/api/users/{userId}/deactivate", created.id())
				.retrieve()
				.body(User.class);
		assertThat(deactivated.status()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	void restApiReturnsNotFoundForUnknownUser() {
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		ResponseEntity<Void> response = client.get()
				.uri("/api/users/{userId}", UUID.randomUUID())
				.retrieve()
				.onStatus(status -> status.value() == 404, (request, rejectedResponse) -> {
				})
				.toBodilessEntity();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void roleLifecycleCreatesUpdatesAndRetires() {
		Role created = roleOperations.create(new CreateRole("  Technician Lifecycle  ", "  Field technician "));
		assertThat(created.name()).isEqualTo("Technician Lifecycle");
		assertThat(created.description()).isEqualTo("Field technician");
		assertThat(created.status()).isEqualTo(RoleStatus.ACTIVE);

		Role updated = roleOperations.update(created.id(), new UpdateRole("Field Technician Lifecycle", "On-site technician"));
		assertThat(updated.name()).isEqualTo("Field Technician Lifecycle");
		assertThat(updated.description()).isEqualTo("On-site technician");

		Role retired = roleOperations.retire(created.id());
		assertThat(retired.status()).isEqualTo(RoleStatus.RETIRED);
		assertThat(roleOperations.findById(created.id())).contains(retired);
	}

	@Test
	void userRoleAssignmentAssignsListsAndRemoves() {
		User user = userOperations.create(new CreateUser(
				"casey.kim",
				"casey@example.com",
				"Casey",
				"Kim",
				null));
		Role administrator = roleOperations.create(new CreateRole("Administrator Assignment", null));
		Role technician = roleOperations.create(new CreateRole("Technician Assignment", null));

		assertThat(userOperations.listRoles(user.id())).isEmpty();

		assertThat(userOperations.assignRole(user.id(), administrator.id())).containsExactly(administrator);
		assertThat(userOperations.assignRole(user.id(), technician.id()))
				.containsExactlyInAnyOrder(administrator, technician);
		assertThat(userOperations.assignRole(user.id(), administrator.id()))
				.containsExactlyInAnyOrder(administrator, technician);

		assertThat(userOperations.removeRole(user.id(), technician.id())).containsExactly(administrator);
		assertThat(userOperations.listRoles(user.id())).containsExactly(administrator);
	}

	@Test
	void assigningARetiredRoleIsRejected() {
		User user = userOperations.create(new CreateUser(
				"devon.wu",
				"devon@example.com",
				"Devon",
				"Wu",
				null));
		Role legacy = roleOperations.create(new CreateRole("Legacy Rejected", null));
		roleOperations.retire(legacy.id());

		assertThatThrownBy(() -> userOperations.assignRole(user.id(), legacy.id()))
				.isInstanceOf(RoleRetiredException.class);
		assertThat(userOperations.listRoles(user.id())).isEmpty();
	}

	@Test
	void restApiCreatesRolesAndManagesUserAssignments() {
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		ResponseEntity<Role> roleResponse = client.post()
				.uri("/api/roles")
				.body(new CreateRoleRequest("Manager Rest", "Oversees daily work"))
				.retrieve()
				.toEntity(Role.class);
		assertThat(roleResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Role role = roleResponse.getBody();
		assertThat(role).isNotNull();

		User user = client.post()
				.uri("/api/users")
				.body(new CreateRequest("taylor.n", "taylor@example.com", "Taylor", "N", null))
				.retrieve()
				.body(User.class);
		assertThat(user).isNotNull();

		Role[] assigned = client.post()
				.uri("/api/users/{userId}/roles/{roleId}", user.id(), role.id())
				.retrieve()
				.body(Role[].class);
		assertThat(assigned).containsExactly(role);

		Role[] roles = client.get()
				.uri("/api/users/{userId}/roles", user.id())
				.retrieve()
				.body(Role[].class);
		assertThat(roles).containsExactly(role);

		Role[] removed = client.delete()
				.uri("/api/users/{userId}/roles/{roleId}", user.id(), role.id())
				.retrieve()
				.body(Role[].class);
		assertThat(removed).isEmpty();
	}

	@Test
	void groupLifecycleCreatesUpdatesAndRetires() {
		Group created = groupOperations.create(new CreateGroup("  Field Crew Lifecycle  ", "  Field team "));
		assertThat(created.name()).isEqualTo("Field Crew Lifecycle");
		assertThat(created.description()).isEqualTo("Field team");
		assertThat(created.status()).isEqualTo(GroupStatus.ACTIVE);

		Group updated = groupOperations.update(created.id(), new UpdateGroup("Field Crew Lifecycle Updated", "Updated team"));
		assertThat(updated.name()).isEqualTo("Field Crew Lifecycle Updated");
		assertThat(updated.description()).isEqualTo("Updated team");

		Group retired = groupOperations.retire(created.id());
		assertThat(retired.status()).isEqualTo(GroupStatus.RETIRED);
		assertThat(groupOperations.findById(created.id())).contains(retired);
	}

	@Test
	void userGroupMembershipAssignsListsAndRemoves() {
		User user = userOperations.create(new CreateUser(
				"jamie.reyes",
				"jamie@example.com",
				"Jamie",
				"Reyes",
				null));
		Group north = groupOperations.create(new CreateGroup("North Crew Assignment", null));
		Group south = groupOperations.create(new CreateGroup("South Crew Assignment", null));

		assertThat(userOperations.listGroups(user.id())).isEmpty();

		assertThat(userOperations.assignGroup(user.id(), north.id())).containsExactly(north);
		assertThat(userOperations.assignGroup(user.id(), south.id()))
				.containsExactlyInAnyOrder(north, south);
		assertThat(userOperations.assignGroup(user.id(), north.id()))
				.containsExactlyInAnyOrder(north, south);

		assertThat(userOperations.removeGroup(user.id(), south.id())).containsExactly(north);
		assertThat(userOperations.listGroups(user.id())).containsExactly(north);
	}

	@Test
	void assigningARetiredGroupIsRejected() {
		User user = userOperations.create(new CreateUser(
				"charlie.bell",
				"charlie@example.com",
				"Charlie",
				"Bell",
				null));
		Group legacy = groupOperations.create(new CreateGroup("Legacy Crew Rejected", null));
		groupOperations.retire(legacy.id());

		assertThatThrownBy(() -> userOperations.assignGroup(user.id(), legacy.id()))
				.isInstanceOf(GroupRetiredException.class);
		assertThat(userOperations.listGroups(user.id())).isEmpty();
	}

	@Test
	void restApiCreatesGroupsAndManagesMembership() {
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		ResponseEntity<Group> groupResponse = client.post()
				.uri("/api/groups")
				.body(new CreateGroupRequest("Night Crew Rest", "Night shift"))
				.retrieve()
				.toEntity(Group.class);
		assertThat(groupResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Group group = groupResponse.getBody();
		assertThat(group).isNotNull();

		User user = client.post()
				.uri("/api/users")
				.body(new CreateRequest("taylor.m", "taylor.m@example.com", "Taylor", "M", null))
				.retrieve()
				.body(User.class);
		assertThat(user).isNotNull();

		Group[] assigned = client.post()
				.uri("/api/users/{userId}/groups/{groupId}", user.id(), group.id())
				.retrieve()
				.body(Group[].class);
		assertThat(assigned).containsExactly(group);

		Group[] groups = client.get()
				.uri("/api/users/{userId}/groups", user.id())
				.retrieve()
				.body(Group[].class);
		assertThat(groups).containsExactly(group);

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
	void mutationsRecordAuditEventsAndAuditCanBeFiltered() {
		User user = userOperations.create(new CreateUser(
				"audit.user",
				"audit@example.com",
				"Audit",
				"User",
				null));
		Role role = roleOperations.create(new CreateRole("Audit Role", null));
		userOperations.assignRole(user.id(), role.id());
		userOperations.update(user.id(), new UpdateUser(
				"audit.updated@example.com",
				"Audit",
				"User",
				"555-0000"));
		userOperations.deactivate(user.id());

		List<UserAuditEvent> userEvents = auditOperations.search(new AuditFilter(
				null, AuditActions.SUBJECT_USER, user.id().toString(), null, null, null, 100));

		assertThat(userEvents).extracting(UserAuditEvent::actor).containsOnly("system");
		assertThat(userEvents).extracting(UserAuditEvent::action).contains(
				AuditActions.USER_CREATED,
				AuditActions.USER_UPDATED,
				AuditActions.USER_DEACTIVATED,
				AuditActions.ROLE_ASSIGNED);

		assertThat(auditOperations.search(new AuditFilter(
				null, null, null, AuditActions.ROLE_ASSIGNED, null, null, 100)))
				.extracting(UserAuditEvent::action).containsOnly(AuditActions.ROLE_ASSIGNED);

		for (UserAuditEvent event : userEvents) {
			assertThat(event.changedFields())
					.doesNotContain("audit@example.com", "audit.updated@example.com", "555-0000");
		}
	}

	@Test
	void personalFieldsAreEncryptedAtRestAndMaskedInTheList() {
		User created = userOperations.create(new CreateUser(
				"encrypt.me",
				"encrypt.me@example.com",
				"Encrypt",
				"Me",
				"555-7777"));

		String dbEmail = jdbcTemplate.queryForObject(
				"select email from sbp_user where id = ?", String.class, created.id());
		String dbGivenName = jdbcTemplate.queryForObject(
				"select given_name from sbp_user where id = ?", String.class, created.id());
		String dbPhone = jdbcTemplate.queryForObject(
				"select phone from sbp_user where id = ?", String.class, created.id());

		assertThat(dbEmail).isNotEqualTo("encrypt.me@example.com").doesNotContain("encrypt.me@example.com");
		assertThat(dbGivenName).doesNotContain("Encrypt");
		assertThat(dbPhone).doesNotContain("555-7777");

		assertThat(userOperations.findById(created.id()).orElseThrow().email())
				.isEqualTo("encrypt.me@example.com");

		RestClient client = RestClient.create("http://localhost:" + serverPort);
		User[] listed = client.get()
				.uri("/api/users?query=encrypt.me")
				.retrieve()
				.body(User[].class);

		assertThat(listed).hasSize(1);
		assertThat(listed[0].email()).isEqualTo("e***@example.com");
		assertThat(listed[0].phone()).isEqualTo("***7777");
		assertThat(listed[0].givenName()).isEqualTo("Encrypt");
	}

	@Test
	void nameAndEmailSearchWorkThroughBlindIndexesWithoutPlaintext() {
		User created = userOperations.create(new CreateUser(
				"search.jane",
				"Jane.Doe@Example.com",
				"Jane",
				"Doe",
				null));

		assertThat(userOperations.search(new UserFilter("jane.doe@example.com", null, null, null)))
				.containsExactly(created);
		assertThat(userOperations.search(new UserFilter("JANE", null, null, null))).containsExactly(created);
		assertThat(userOperations.search(new UserFilter("doe", null, null, null))).containsExactly(created);
		assertThat(userOperations.search(new UserFilter("jane doe", null, null, null))).containsExactly(created);
		assertThat(userOperations.search(new UserFilter("search.jane", null, null, null))).containsExactly(created);

		String emailToken = jdbcTemplate.queryForObject(
				"select email_token from sbp_user where id = ?", String.class, created.id());
		String givenNameToken = jdbcTemplate.queryForObject(
				"select given_name_token from sbp_user where id = ?", String.class, created.id());
		assertThat(emailToken).matches("[0-9a-f]{64}").doesNotContain("jane", "doe", "example.com");
		assertThat(givenNameToken).matches("[0-9a-f]{64}").doesNotContain("jane");
	}

	@Test
	void userListFiltersByRoleAndGroup() {
		User user = userOperations.create(new CreateUser(
				"filter.me",
				"filter.me@example.com",
				"Filter",
				"Me",
				null));
		Role role = roleOperations.create(new CreateRole("Filter Role", null));
		Group group = groupOperations.create(new CreateGroup("Filter Group", null));
		userOperations.assignRole(user.id(), role.id());
		userOperations.assignGroup(user.id(), group.id());

		assertThat(userOperations.search(new UserFilter(null, null, role.id(), null)))
				.containsExactly(user);
		assertThat(userOperations.search(new UserFilter(null, null, null, group.id())))
				.containsExactly(user);
		assertThat(userOperations.search(new UserFilter(null, null, UUID.randomUUID(), null)))
				.isEmpty();
	}

	@Test
	void encryptionPropertiesRequireDistinctKeys() {
		assertThatThrownBy(() -> new com.oneilsoftware.sbp.module.usermaintenance.internal.UserMaintenanceProperties("key", ""))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new com.oneilsoftware.sbp.module.usermaintenance.internal.UserMaintenanceProperties("same", "same"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void permissionCatalogDefinesGrantsAndLists() {
		permissionOperations.define("test.customer.read", "View customers");
		Permission updated = permissionOperations.define("test.customer.read", "View customer records");

		Role role = roleOperations.create(new CreateRole("Permission Catalog Role", null));
		permissionOperations.grant(role.id(), "test.customer.read");

		assertThat(updated.description()).isEqualTo("View customer records");
		assertThat(permissionOperations.list()).extracting(Permission::key).contains("test.customer.read");
		assertThat(permissionOperations.listByRole(role.id())).extracting(Permission::key).containsExactly("test.customer.read");
		assertThat(permissionOperations.listRolesByPermission("test.customer.read")).extracting(Role::name)
				.containsExactly("Permission Catalog Role");

		permissionOperations.revoke(role.id(), "test.customer.read");
		assertThat(permissionOperations.listByRole(role.id())).isEmpty();
	}

	@Test
	void bulkImportCreatesValidRowsAndReportsPerRowErrors() {
		List<BulkCreateUser> rows = List.of(
				new BulkCreateUser("bulk.one", "bulk.one@example.com", "Bulk", "One", null),
				new BulkCreateUser("", "invalid@example.com", "Invalid", "Row", null),
				new BulkCreateUser("bulk.two", "bulk.two@example.com", "Bulk", "Two", null),
				new BulkCreateUser("bulk.one", "dup@example.com", "Dup", "One", null));

		BulkUserImportResult result = userOperations.bulkCreate(rows);

		assertThat(result.created()).extracting(User::username).containsExactly("bulk.one", "bulk.two");
		assertThat(result.errors()).hasSize(2);
		assertThat(result.errors().get(0).index()).isEqualTo(1);
		assertThat(result.errors().get(0).message()).contains("Username is required");
		assertThat(result.errors().get(1).index()).isEqualTo(3);
		assertThat(result.errors().get(1).message()).contains("already exists");
	}

	@Test
	void exportReturnsMaskedUsers() {
		userOperations.create(new CreateUser("export.me", "export.me@example.com", "Export", "Me", "555-1111"));

		RestClient client = RestClient.create("http://localhost:" + serverPort);
		User[] exported = client.get()
				.uri("/api/users/export")
				.retrieve()
				.body(User[].class);

		assertThat(exported).anyMatch(user -> user.username().equals("export.me")
				&& user.email().equals("e***@example.com")
				&& user.phone().equals("***1111"));
	}

	@Test
	void groupRoleAssignmentAssignsListsAndRemoves() {
		Group group = groupOperations.create(new CreateGroup("Field Crew Roles", null));
		Role administrator = roleOperations.create(new CreateRole("Group Admin Role", null));
		Role technician = roleOperations.create(new CreateRole("Group Tech Role", null));

		assertThat(groupOperations.listRoles(group.id())).isEmpty();

		assertThat(groupOperations.assignRole(group.id(), administrator.id())).containsExactly(administrator);
		assertThat(groupOperations.assignRole(group.id(), technician.id()))
				.containsExactlyInAnyOrder(administrator, technician);
		assertThat(groupOperations.assignRole(group.id(), administrator.id()))
				.containsExactlyInAnyOrder(administrator, technician);

		assertThat(groupOperations.removeRole(group.id(), technician.id())).containsExactly(administrator);
		assertThat(groupOperations.listRoles(group.id())).containsExactly(administrator);
	}

	@Test
	void groupRoleAssignmentRejectsRetiredRoleAndRetiredGroup() {
		Group group = groupOperations.create(new CreateGroup("Grant Reject Group", null));
		Role retiredRole = roleOperations.create(new CreateRole("Grant Retired Role", null));
		roleOperations.retire(retiredRole.id());
		assertThatThrownBy(() -> groupOperations.assignRole(group.id(), retiredRole.id()))
				.isInstanceOf(RoleRetiredException.class);

		Role activeRole = roleOperations.create(new CreateRole("Grant Active Role", null));
		Group retiredGroup = groupOperations.create(new CreateGroup("Grant Retired Group", null));
		groupOperations.retire(retiredGroup.id());
		assertThatThrownBy(() -> groupOperations.assignRole(retiredGroup.id(), activeRole.id()))
				.isInstanceOf(GroupRetiredException.class);

		assertThat(groupOperations.listRoles(group.id())).isEmpty();
		assertThat(groupOperations.listRoles(retiredGroup.id())).isEmpty();
	}

	@Test
	void userEffectiveRolesCombineDirectAndInheritedAndDeduplicate() {
		User joe = userOperations.create(new CreateUser(
				"joe.effective", "joe@example.com", "Joe", "Effective", null));
		Role administrator = roleOperations.create(new CreateRole("Administrator Effective", null));
		Role reportViewer = roleOperations.create(new CreateRole("Report Viewer Effective", null));
		Role customerManager = roleOperations.create(new CreateRole("Customer Manager Effective", null));
		Group groupA = groupOperations.create(new CreateGroup("Group A Effective", null));
		Group groupB = groupOperations.create(new CreateGroup("Group B Effective", null));

		userOperations.assignRole(joe.id(), administrator.id());
		userOperations.assignGroup(joe.id(), groupA.id());
		userOperations.assignGroup(joe.id(), groupB.id());
		groupOperations.assignRole(groupA.id(), administrator.id());
		groupOperations.assignRole(groupA.id(), reportViewer.id());
		groupOperations.assignRole(groupB.id(), reportViewer.id());
		groupOperations.assignRole(groupB.id(), customerManager.id());

		assertThat(userOperations.listRoles(joe.id())).containsExactly(administrator);
		assertThat(userOperations.listEffectiveRoles(joe.id())).extracting(Role::name)
				.containsExactly("Administrator Effective", "Customer Manager Effective", "Report Viewer Effective");
	}

	@Test
	void removingUserFromGroupRemovesInheritanceButKeepsDirectRoles() {
		User user = userOperations.create(new CreateUser(
				"leave.group", "leave@example.com", "Leave", "Group", null));
		Role role = roleOperations.create(new CreateRole("Leave Group Role", null));
		Group group = groupOperations.create(new CreateGroup("Leave Group", null));
		userOperations.assignGroup(user.id(), group.id());
		groupOperations.assignRole(group.id(), role.id());

		assertThat(userOperations.listEffectiveRoles(user.id())).containsExactly(role);

		userOperations.removeGroup(user.id(), group.id());
		assertThat(userOperations.listEffectiveRoles(user.id())).isEmpty();

		userOperations.assignRole(user.id(), role.id());
		assertThat(userOperations.listEffectiveRoles(user.id())).containsExactly(role);
	}

	@Test
	void removingRoleFromGroupRemovesInheritance() {
		User user = userOperations.create(new CreateUser(
				"lost.role", "lost@example.com", "Lost", "Role", null));
		Role role = roleOperations.create(new CreateRole("Lost Role", null));
		Group group = groupOperations.create(new CreateGroup("Lost Role Group", null));
		userOperations.assignGroup(user.id(), group.id());
		groupOperations.assignRole(group.id(), role.id());

		assertThat(userOperations.listEffectiveRoles(user.id())).containsExactly(role);

		groupOperations.removeRole(group.id(), role.id());
		assertThat(userOperations.listEffectiveRoles(user.id())).isEmpty();
	}

	@Test
	void retiringRoleKeepsAssignmentsButExcludesItFromEffectiveRoles() {
		User user = userOperations.create(new CreateUser(
				"retired.role", "retired.role@example.com", "Retired", "Role", null));
		Role role = roleOperations.create(new CreateRole("Retiring Role", null));
		Group group = groupOperations.create(new CreateGroup("Retiring Role Group", null));
		userOperations.assignRole(user.id(), role.id());
		userOperations.assignGroup(user.id(), group.id());
		groupOperations.assignRole(group.id(), role.id());

		assertThat(userOperations.listEffectiveRoles(user.id())).containsExactly(role);

		roleOperations.retire(role.id());

		assertThat(groupOperations.listRoles(group.id())).extracting(Role::name).containsExactly("Retiring Role");
		assertThat(userOperations.listRoles(user.id())).extracting(Role::name).containsExactly("Retiring Role");
		assertThat(userOperations.listEffectiveRoles(user.id())).isEmpty();
	}

	@Test
	void retiringGroupKeepsAssignmentsButExcludesItsGrantsFromEffectiveRoles() {
		User user = userOperations.create(new CreateUser(
				"retired.group", "retired.group@example.com", "Retired", "Group", null));
		Role role = roleOperations.create(new CreateRole("Group Grant Role", null));
		Group group = groupOperations.create(new CreateGroup("Retiring Group", null));
		userOperations.assignGroup(user.id(), group.id());
		groupOperations.assignRole(group.id(), role.id());

		assertThat(userOperations.listEffectiveRoles(user.id())).containsExactly(role);

		groupOperations.retire(group.id());

		assertThat(groupOperations.listRoles(group.id())).extracting(Role::name).containsExactly("Group Grant Role");
		assertThat(userOperations.listEffectiveRoles(user.id())).isEmpty();
	}

	@Test
	void groupRoleMutationsRecordAuditEvents() {
		Group group = groupOperations.create(new CreateGroup("Audit Grant Group", null));
		Role role = roleOperations.create(new CreateRole("Audit Grant Role", null));

		groupOperations.assignRole(group.id(), role.id());
		groupOperations.removeRole(group.id(), role.id());

		List<UserAuditEvent> groupEvents = auditOperations.search(new AuditFilter(
				null, AuditActions.SUBJECT_GROUP, group.id().toString(), null, null, null, 100));
		assertThat(groupEvents).extracting(UserAuditEvent::action).contains(
				AuditActions.GROUP_ROLE_ASSIGNED,
				AuditActions.GROUP_ROLE_REMOVED);
		assertThat(groupEvents).extracting(UserAuditEvent::actor).containsOnly("system");
		for (UserAuditEvent event : groupEvents) {
			assertThat(event.changedFields()).doesNotContain(group.name(), role.name());
		}
	}

	@Test
	void restApiManagesGroupRolesAndEffectiveRoles() {
		RestClient client = RestClient.create("http://localhost:" + serverPort);

		Role role = client.post()
				.uri("/api/roles")
				.body(new CreateRoleRequest("Group Grant Rest", "Granted at group level"))
				.retrieve()
				.body(Role.class);
		Group group = client.post()
				.uri("/api/groups")
				.body(new CreateGroupRequest("Grant Crew Rest", "Crew with grants"))
				.retrieve()
				.body(Group.class);
		User user = client.post()
				.uri("/api/users")
				.body(new CreateRequest("grant.rest", "grant.rest@example.com", "Grant", "Rest", null))
				.retrieve()
				.body(User.class);
		assertThat(role).isNotNull();
		assertThat(group).isNotNull();
		assertThat(user).isNotNull();

		Role[] assigned = client.post()
				.uri("/api/groups/{groupId}/roles/{roleId}", group.id(), role.id())
				.retrieve()
				.body(Role[].class);
		assertThat(assigned).containsExactly(role);

		Role[] roles = client.get()
				.uri("/api/groups/{groupId}/roles", group.id())
				.retrieve()
				.body(Role[].class);
		assertThat(roles).containsExactly(role);

		client.post()
				.uri("/api/users/{userId}/groups/{groupId}", user.id(), group.id())
				.retrieve()
				.toBodilessEntity();

		Role[] effective = client.get()
				.uri("/api/users/{userId}/effective-roles", user.id())
				.retrieve()
				.body(Role[].class);
		assertThat(effective).containsExactly(role);

		Role[] removed = client.delete()
				.uri("/api/groups/{groupId}/roles/{roleId}", group.id(), role.id())
				.retrieve()
				.body(Role[].class);
		assertThat(removed).isEmpty();

		Role[] effectiveAfterRemoval = client.get()
				.uri("/api/users/{userId}/effective-roles", user.id())
				.retrieve()
				.body(Role[].class);
		assertThat(effectiveAfterRemoval).isEmpty();
	}

	private record CreateRequest(
			String username,
			String email,
			String givenName,
			String familyName,
			String phone) {
	}

	private record UpdateRequest(
			String email,
			String givenName,
			String familyName,
			String phone) {
	}

	private record CreateRoleRequest(String name, String description) {
	}

	private record CreateGroupRequest(String name, String description) {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}
}
