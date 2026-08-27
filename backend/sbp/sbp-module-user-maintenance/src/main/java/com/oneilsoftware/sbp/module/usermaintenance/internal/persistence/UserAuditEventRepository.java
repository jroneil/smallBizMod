package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAuditEventRepository extends JpaRepository<UserAuditEventEntity, UUID> {

	@Query("""
			select e from UserAuditEventEntity e
			where (:actor is null or e.actor = :actor)
			  and (:subjectType is null or e.subjectType = :subjectType)
			  and (:subjectId is null or e.subjectId = :subjectId)
			  and (:action is null or e.action = :action)
			  and (cast(:from as timestamp) is null or e.occurredAt >= :from)
			  and (cast(:to as timestamp) is null or e.occurredAt <= :to)
			order by e.occurredAt desc
			""")
	List<UserAuditEventEntity> search(
			@Param("actor") String actor,
			@Param("subjectType") String subjectType,
			@Param("subjectId") String subjectId,
			@Param("action") String action,
			@Param("from") Instant from,
			@Param("to") Instant to,
			Pageable pageable);
}
