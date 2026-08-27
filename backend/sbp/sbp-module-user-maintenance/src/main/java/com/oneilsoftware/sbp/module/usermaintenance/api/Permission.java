package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.UUID;

/**
 * A customer-defined permission in the read-only catalog. The permission key and description are
 * owned by customer security policy; the module stores and displays them without hardcoding any
 * universal permission set (framework P-01: capability over policy).
 */
public record Permission(UUID id, String key, String description) {
}
