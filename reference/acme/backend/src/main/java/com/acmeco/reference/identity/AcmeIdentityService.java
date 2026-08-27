package com.acmeco.reference.identity;

import java.security.Principal;
import java.util.Optional;

import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserOperations;
import org.springframework.stereotype.Component;

/**
 * Customer-owned mapping from the authenticated identity to an SBP User.
 *
 * <p>Acme's reference policy aligns the authenticated username with the SBP User username. This is
 * a customer decision: SBP exposes a neutral {@code findByUsername} lookup and does not know how
 * authentication happens. A future external IdP whose subject differs from the SBP username would
 * replace this alignment with a customer-owned identity-mapping table.
 */
@Component
public class AcmeIdentityService {

	private final UserOperations userOperations;

	public AcmeIdentityService(UserOperations userOperations) {
		this.userOperations = userOperations;
	}

	public Optional<User> currentUser(Principal principal) {
		if (principal == null) {
			return Optional.empty();
		}
		return userOperations.findByUsername(principal.getName());
	}
}
