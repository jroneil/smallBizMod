package com.oneilsoftware.sbp.module.job.internal.persistence;

import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.job.api.JobPriority;
import com.oneilsoftware.sbp.module.job.api.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

	@Query("""
			select j from JobEntity j
			where (:query is null
			        or lower(j.title) like concat('%', lower(cast(:query as string)), '%')
			        or lower(j.description) like concat('%', lower(cast(:query as string)), '%'))
			  and (:customerId is null or j.customerId = :customerId)
			  and (:status is null or j.status = :status)
			  and (:priority is null or j.priority = :priority)
			order by j.createdAt desc
			""")
	Page<JobEntity> search(
			@Param("query") String query,
			@Param("customerId") UUID customerId,
			@Param("status") JobStatus status,
			@Param("priority") JobPriority priority,
			Pageable pageable);

	List<JobEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
