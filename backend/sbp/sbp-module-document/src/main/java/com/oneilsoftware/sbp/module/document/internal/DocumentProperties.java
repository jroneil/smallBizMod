package com.oneilsoftware.sbp.module.document.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Document module technical settings. {@code maxSizeBytes} is the authoritative upload size limit
 * (NFR-DOC-07): content over this limit is rejected before anything is stored, and the module's
 * web-layer multipart configuration accepts files up to the same limit. Defaults to 10 MB when
 * {@code sbp.document.max-size-bytes} is not configured.
 */
@ConfigurationProperties("sbp.document")
public record DocumentProperties(long maxSizeBytes) {

	public static final long DEFAULT_MAX_SIZE_BYTES = 10L * 1024 * 1024;

	public DocumentProperties {
		if (maxSizeBytes <= 0) {
			maxSizeBytes = DEFAULT_MAX_SIZE_BYTES;
		}
	}
}
