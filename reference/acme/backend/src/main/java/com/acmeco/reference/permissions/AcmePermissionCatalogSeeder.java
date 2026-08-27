package com.acmeco.reference.permissions;

import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.CreateRole;
import com.oneilsoftware.sbp.module.usermaintenance.api.PermissionOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleOperations;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Customer-owned permission catalog seed. The reusable User module stores and displays permissions
 * but does not hardcode any universal set; Acme defines its own keys and grants them to its own
 * "Administrator" role here (framework P-01: capability over policy).
 */
@Component
public class AcmePermissionCatalogSeeder implements ApplicationRunner {

	private final PermissionOperations permissionOperations;
	private final RoleOperations roleOperations;

	public AcmePermissionCatalogSeeder(
			PermissionOperations permissionOperations,
			RoleOperations roleOperations) {
		this.permissionOperations = permissionOperations;
		this.roleOperations = roleOperations;
	}

	@Override
	public void run(ApplicationArguments args) {
		permissionOperations.define("acme.customer.read", "View customers");
		permissionOperations.define("acme.customer.write", "Create and edit customers");
		permissionOperations.define("acme.contact.manage", "Manage customer contacts");
		permissionOperations.define("acme.job.read", "View work orders");
		permissionOperations.define("acme.job.manage", "Create and manage work orders");
		permissionOperations.define("acme.document.read", "View and download documents");
		permissionOperations.define("acme.document.manage", "Upload and manage documents");
		permissionOperations.define("acme.report.view", "View reports");

		UUID administratorRoleId = roleOperations.list().stream()
				.filter(role -> role.name().equalsIgnoreCase("Administrator"))
				.map(Role::id)
				.findFirst()
				.orElseGet(() -> roleOperations.create(new CreateRole("Administrator", "Full administrative access")).id());

		permissionOperations.grant(administratorRoleId, "acme.customer.read");
		permissionOperations.grant(administratorRoleId, "acme.customer.write");
		permissionOperations.grant(administratorRoleId, "acme.contact.manage");
		permissionOperations.grant(administratorRoleId, "acme.job.read");
		permissionOperations.grant(administratorRoleId, "acme.job.manage");
		permissionOperations.grant(administratorRoleId, "acme.document.read");
		permissionOperations.grant(administratorRoleId, "acme.document.manage");
		permissionOperations.grant(administratorRoleId, "acme.report.view");
	}
}
