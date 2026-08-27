package com.oneilsoftware.sbp.module.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.oneilsoftware.sbp.module.document.internal.DocumentProperties;
import org.junit.jupiter.api.Test;

class DocumentPropertiesTests {

	@Test
	void defaultsToTenMegabytesWhenNotConfigured() {
		assertThat(new DocumentProperties(0).maxSizeBytes()).isEqualTo(DocumentProperties.DEFAULT_MAX_SIZE_BYTES);
		assertThat(DocumentProperties.DEFAULT_MAX_SIZE_BYTES).isEqualTo(10L * 1024 * 1024);
	}

	@Test
	void keepsAnExplicitlyConfiguredLimit() {
		assertThat(new DocumentProperties(512).maxSizeBytes()).isEqualTo(512);
	}
}
