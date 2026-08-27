package com.oneilsoftware.sbp.module.document.api;

/**
 * Thrown when uploaded content exceeds the module's configured size limit
 * ({@code sbp.document.max-size-bytes}, default 10 MB). The limit is enforced before any content
 * is stored (NFR-DOC-07). Maps to 413 Payload Too Large at the web layer.
 */
public class DocumentSizeLimitExceededException extends RuntimeException {

	public DocumentSizeLimitExceededException(long limitBytes, long actualBytes) {
		super("File content of " + actualBytes + " bytes exceeds the document size limit of "
				+ limitBytes + " bytes");
	}
}
