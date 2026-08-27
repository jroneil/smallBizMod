package com.acmeco.reference.security;

public record AcmeCredentialStatus(
		boolean hasCredential,
		boolean locked,
		int failedAttempts,
		boolean passwordChangeRequired) {
}
