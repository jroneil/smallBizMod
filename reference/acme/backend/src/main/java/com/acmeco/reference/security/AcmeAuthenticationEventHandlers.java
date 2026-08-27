package com.acmeco.reference.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Tracks failed and successful local authentications. Failed attempts increment a per-user counter
 * that locks the account at the threshold; a successful authentication resets the counter.
 */
@Component
public class AcmeAuthenticationEventHandlers {

	private final AcmeCredentialService credentialService;

	public AcmeAuthenticationEventHandlers(AcmeCredentialService credentialService) {
		this.credentialService = credentialService;
	}

	@EventListener
	public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
		credentialService.recordFailedLogin(event.getAuthentication().getName());
	}

	@EventListener
	public void onSuccess(AuthenticationSuccessEvent event) {
		credentialService.recordSuccessfulLogin(event.getAuthentication().getName());
	}
}
