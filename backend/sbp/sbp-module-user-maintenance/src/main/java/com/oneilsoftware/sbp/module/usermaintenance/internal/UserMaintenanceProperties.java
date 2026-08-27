package com.oneilsoftware.sbp.module.usermaintenance.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * User Maintenance technical settings. The encryption key and the separate blind-index search key
 * are supplied by the deployment environment and are never present in source or committed
 * configuration (NFR-UM-02). The two keys must be distinct: the search key never decrypts PII, and
 * the encryption key never produces search tokens.
 */
@ConfigurationProperties("sbp.usermaintenance")
public record UserMaintenanceProperties(String encryptionKey, String searchKey) {

	public UserMaintenanceProperties {
		Assert.hasText(encryptionKey, "sbp.usermaintenance.encryption-key must be configured");
		Assert.hasText(searchKey, "sbp.usermaintenance.search-key must be configured");
		if (encryptionKey.equals(searchKey)) {
			throw new IllegalArgumentException(
					"sbp.usermaintenance.encryption-key and sbp.usermaintenance.search-key must be distinct");
		}
	}
}
