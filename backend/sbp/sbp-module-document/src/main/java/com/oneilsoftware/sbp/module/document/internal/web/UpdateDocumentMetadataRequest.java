package com.oneilsoftware.sbp.module.document.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateDocumentMetadataRequest(
		@NotBlank @Size(max = 255) String displayName) {
}
