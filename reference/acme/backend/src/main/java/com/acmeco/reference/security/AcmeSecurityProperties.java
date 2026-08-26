package com.acmeco.reference.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties("acme.security")
record AcmeSecurityProperties(String username, String password) {

	AcmeSecurityProperties {
		Assert.hasText(username, "acme.security.username must be configured");
		Assert.hasText(password, "acme.security.password must be configured");
	}
}
