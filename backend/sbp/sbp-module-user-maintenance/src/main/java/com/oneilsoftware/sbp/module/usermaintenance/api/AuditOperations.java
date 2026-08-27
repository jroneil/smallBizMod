package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.List;

public interface AuditOperations {

	List<UserAuditEvent> search(AuditFilter filter);
}
