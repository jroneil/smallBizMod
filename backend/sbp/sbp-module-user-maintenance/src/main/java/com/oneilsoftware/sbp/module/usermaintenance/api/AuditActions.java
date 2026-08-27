package com.oneilsoftware.sbp.module.usermaintenance.api;

/**
 * Well-known audit subject types and actions recorded by User Maintenance. Kept as plain strings so
 * customer extensions can add their own actions without changing the module.
 */
public final class AuditActions {

	public static final String SUBJECT_USER = "USER";
	public static final String SUBJECT_ROLE = "ROLE";
	public static final String SUBJECT_GROUP = "GROUP";

	public static final String USER_CREATED = "USER_CREATED";
	public static final String USER_UPDATED = "USER_UPDATED";
	public static final String USER_DEACTIVATED = "USER_DEACTIVATED";
	public static final String USER_ACTIVATED = "USER_ACTIVATED";

	public static final String ROLE_CREATED = "ROLE_CREATED";
	public static final String ROLE_UPDATED = "ROLE_UPDATED";
	public static final String ROLE_RETIRED = "ROLE_RETIRED";

	public static final String GROUP_CREATED = "GROUP_CREATED";
	public static final String GROUP_UPDATED = "GROUP_UPDATED";
	public static final String GROUP_RETIRED = "GROUP_RETIRED";

	public static final String ROLE_ASSIGNED = "ROLE_ASSIGNED";
	public static final String ROLE_REMOVED = "ROLE_REMOVED";
	public static final String GROUP_ASSIGNED = "GROUP_ASSIGNED";
	public static final String GROUP_REMOVED = "GROUP_REMOVED";

	public static final String GROUP_ROLE_ASSIGNED = "GROUP_ROLE_ASSIGNED";
	public static final String GROUP_ROLE_REMOVED = "GROUP_ROLE_REMOVED";

	private AuditActions() {
	}
}
