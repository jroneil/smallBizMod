package com.acmeco.reference.identity;

import com.acmeco.reference.security.AcmeCredentialService;
import com.acmeco.reference.security.AcmeSecurityProperties;
import com.oneilsoftware.sbp.module.usermaintenance.api.CreateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserOperations;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Customer-owned bootstrap that ensures the Acme development/demo security user has an SBP User
 * record and a local credential seeded from the environment-defined demo password. This keeps the
 * reference "My Profile" and sign-in flows demonstrable without moving authentication or seeding
 * policy into the reusable User module.
 */
@Component
public class AcmeDemoUserBootstrap implements ApplicationRunner {

	private final UserOperations userOperations;
	private final AcmeSecurityProperties securityProperties;
	private final AcmeCredentialService credentialService;

	public AcmeDemoUserBootstrap(
			UserOperations userOperations,
			AcmeSecurityProperties securityProperties,
			AcmeCredentialService credentialService) {
		this.userOperations = userOperations;
		this.securityProperties = securityProperties;
		this.credentialService = credentialService;
	}

	@Override
	public void run(ApplicationArguments args) {
		String username = securityProperties.username();
		User user = userOperations.findByUsername(username)
				.orElseGet(() -> userOperations.create(new CreateUser(
						username,
						username + "@acme.example",
						"Acme",
						"Demo User",
						null)));
		credentialService.ensureCredential(user.id(), securityProperties.password());
	}
}
