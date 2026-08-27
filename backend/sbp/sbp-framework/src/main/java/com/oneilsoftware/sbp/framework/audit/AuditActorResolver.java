package com.oneilsoftware.sbp.framework.audit;

/**
 * Supplies the current actor identity for audit events. The customer application provides the real
 * authenticated identity; reusable modules depend only on this contract, never on a specific
 * authentication mechanism or security policy.
 */
@FunctionalInterface
public interface AuditActorResolver {

	String currentActor();
}
