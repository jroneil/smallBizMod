package com.oneilsoftware.sbp.module.document.api;

import java.util.UUID;

/**
 * Thrown when a Document operation references a Document id that does not exist. Maps to 404 at
 * the web layer.
 */
public class UnknownDocumentException extends RuntimeException {

	public UnknownDocumentException(UUID documentId) {
		super("Unknown document: " + documentId);
	}
}
