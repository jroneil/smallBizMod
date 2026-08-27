package com.oneilsoftware.sbp.module.document.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Document-owned audit persistence (ADR-DOC-004). Events are written through
 * {@code DocumentAuditRecorder} in the same transaction as the business change. The read surface
 * for a Document's audit history (DOC-FR-10) queries by the stable Document subject id, newest
 * first, optionally narrowed by action, and does not require the Document row to still exist.
 */
public interface DocumentAuditEventRepository extends JpaRepository<DocumentAuditEventEntity, UUID> {

	@Query("""
			select e from DocumentAuditEventEntity e
			where e.subjectType = 'DOCUMENT'
			  and e.subjectId = :subjectId
			  and (:action is null or e.action = :action)
			order by e.occurredAt desc
			""")
	List<DocumentAuditEventEntity> findBySubjectId(
			@Param("subjectId") String subjectId,
			@Param("action") String action);
}
