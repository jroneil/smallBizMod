package com.oneilsoftware.sbp.module.document.internal.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

	/**
	 * Paginated Document search (DOC-FR-02): case-insensitive substring match on the display
	 * name, plus optional owner narrowing expressed as the owning column ({@code customerId} for
	 * a Customer owner, {@code jobId} for a Job owner), most recently created first.
	 */
	@Query("""
			select d from DocumentEntity d
			where (:query is null
			        or lower(d.displayName) like concat('%', lower(cast(:query as string)), '%'))
			  and (:customerId is null or d.customerId = :customerId)
			  and (:jobId is null or d.jobId = :jobId)
			order by d.createdAt desc
			""")
	Page<DocumentEntity> search(
			@Param("query") String query,
			@Param("customerId") UUID customerId,
			@Param("jobId") UUID jobId,
			Pageable pageable);
}
