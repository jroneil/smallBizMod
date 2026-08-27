package com.acmeco.reference.audit;

import com.oneilsoftware.sbp.framework.audit.AuditActorResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Customer-owned audit actor resolver: supplies the authenticated username as the audit actor.
 * This keeps Spring Security and the identity policy in Acme while the reusable User module depends
 * only on the framework {@link AuditActorResolver} contract.
 */
@Component
public class AcmeAuditActorResolver implements AuditActorResolver {

	@Override
	public String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "anonymous";
		}
		return authentication.getName();
	}
}
