package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import java.util.Arrays;
import java.util.List;

import com.oneilsoftware.sbp.module.usermaintenance.api.AuditFilter;
import com.oneilsoftware.sbp.module.usermaintenance.api.AuditOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserAuditEvent;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserAuditEventEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserAuditEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

public class AuditApplicationService implements AuditOperations {

	private static final int DEFAULT_LIMIT = 100;
	private static final int MAX_LIMIT = 500;

	private final UserAuditEventRepository repository;

	public AuditApplicationService(UserAuditEventRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserAuditEvent> search(AuditFilter filter) {
		int limit = filter.limit() == null
				? DEFAULT_LIMIT
				: Math.min(Math.max(filter.limit(), 1), MAX_LIMIT);

		return repository.search(
				filter.actor(),
				filter.subjectType(),
				filter.subjectId(),
				filter.action(),
				filter.from(),
				filter.to(),
				PageRequest.of(0, limit)).stream()
				.map(AuditApplicationService::toEvent)
				.toList();
	}

	private static UserAuditEvent toEvent(UserAuditEventEntity entity) {
		return new UserAuditEvent(
				entity.getId(),
				entity.getActor(),
				entity.getSubjectType(),
				entity.getSubjectId(),
				entity.getAction(),
				split(entity.getChangedFields()),
				entity.getDetail(),
				entity.getOccurredAt());
	}

	private static List<String> split(String joined) {
		if (joined == null || joined.isBlank()) {
			return List.of();
		}
		return Arrays.asList(joined.split(","));
	}
}
