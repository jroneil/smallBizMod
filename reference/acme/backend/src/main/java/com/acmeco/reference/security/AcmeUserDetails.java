package com.acmeco.reference.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Acme-owned {@code UserDetails} that carries the mandatory password-change flag so the security
 * boundary can enforce it without another database lookup.
 */
public class AcmeUserDetails extends User {

	private final boolean passwordChangeRequired;

	public AcmeUserDetails(
			String username,
			String password,
			boolean accountNonLocked,
			boolean passwordChangeRequired,
			Collection<? extends GrantedAuthority> authorities) {
		super(username, password, true, true, true, accountNonLocked, authorities);
		this.passwordChangeRequired = passwordChangeRequired;
	}

	public boolean isPasswordChangeRequired() {
		return passwordChangeRequired;
	}
}
