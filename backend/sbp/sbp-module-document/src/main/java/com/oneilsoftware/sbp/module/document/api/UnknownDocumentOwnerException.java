package com.oneilsoftware.sbp.module.document.api;

import java.util.UUID;

/**
 * Thrown when a Document is created for an owner (Customer or Job) that does not exist. Owner
 * identifiers are validated only through the public Customer and Job contracts; this exception
 * maps to 404 at the web layer.
 */
public class UnknownDocumentOwnerException extends RuntimeException {

	public UnknownDocumentOwnerException(DocumentOwnerType ownerType, UUID ownerId) {
		super("Unknown document owner: " + ownerType + " " + ownerId);
	}
}
