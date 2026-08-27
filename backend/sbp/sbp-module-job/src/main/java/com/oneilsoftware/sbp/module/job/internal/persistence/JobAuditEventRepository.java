package com.oneilsoftware.sbp.module.job.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Job-owned audit persistence (ADR-JOB-003). Events are written through {@code JobAuditRecorder}
 * in the same transaction as the business change. The read surface for a Job's audit history
 * (JOB-FR-09) queries by the stable Job subject id, newest first, optionally narrowed by action.
 */
public interface JobAuditEventRepository extends JpaRepository<JobAuditEventEntity, UUID> {

	@Query("""
			select e from JobAuditEventEntity e
			where e.subjectType = 'JOB'
			  and e.subjectId = :subjectId
			  and (:action is null or e.action = :action)
			order by e.occurredAt desc
			""")
	List<JobAuditEventEntity> findBySubjectId(
			@Param("subjectId") String subjectId,
			@Param("action") String action);
}
