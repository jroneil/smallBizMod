package com.acmeco.reference.security;

import org.springframework.stereotype.Component;

/**
 * Acme's reference password policy. This is the single authoritative server-side rule applied to
 * manual resets, generated resets, and self-service changes. It is a customer-owned default, not a
 * reusable SBP requirement.
 */
@Component
public final class AcmePasswordPolicy {

	static final int MIN_LENGTH = 8;
	static final int MAX_LENGTH = 72;

	public void validate(String password) {
		if (password == null || password.length() < MIN_LENGTH) {
			throw new InvalidPasswordException("Password must be at least " + MIN_LENGTH + " characters");
		}
		if (password.length() > MAX_LENGTH) {
			throw new InvalidPasswordException("Password must not exceed " + MAX_LENGTH + " characters");
		}
	}
}
