package com.oneilsoftware.sbp.module.document.api;

/**
 * The type of business record a Document is attached to (DOC-FR-07, ADR-DOC-001). v0.1 supports
 * exactly two owner types — Customer and Job — and a Document has exactly one owner.
 */
public enum DocumentOwnerType {
	CUSTOMER,
	JOB
}
