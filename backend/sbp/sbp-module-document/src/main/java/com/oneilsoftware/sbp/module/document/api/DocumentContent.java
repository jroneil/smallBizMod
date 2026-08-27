package com.oneilsoftware.sbp.module.document.api;

/**
 * The downloadable content of a Document (DOC-FR-04): the stored bytes plus the advisory content
 * type and the display name used for the download's attachment disposition.
 */
public record DocumentContent(byte[] content, String contentType, String displayName) {

	public DocumentContent {
		content = content == null ? new byte[0] : content;
	}
}
