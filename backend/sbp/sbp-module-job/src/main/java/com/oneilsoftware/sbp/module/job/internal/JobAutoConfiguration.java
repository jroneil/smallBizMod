package com.oneilsoftware.sbp.module.job.internal;

import com.oneilsoftware.sbp.framework.audit.AuditActorResolver;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.job.api.JobOperations;
import com.oneilsoftware.sbp.module.job.internal.application.JobApplicationService;
import com.oneilsoftware.sbp.module.job.internal.application.JobAuditRecorder;
import com.oneilsoftware.sbp.module.job.internal.application.JobAuditSupport;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobAuditEventEntity;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobAuditEventRepository;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobEntity;
import com.oneilsoftware.sbp.module.job.internal.persistence.JobRepository;
import com.oneilsoftware.sbp.module.job.internal.web.JobController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Activates the Job capability when the {@code sbp-module-job} JAR is on the classpath (ADR-002).
 * The audit recorder is exposed under its concrete type {@link JobAuditRecorder} so it never
 * competes with User Maintenance's {@code AuditRecorder} bean; the framework audit hook contract is
 * still implemented (ADR-JOB-003). The default {@code AuditActorResolver} backs off when the
 * customer application supplies its own resolver.
 */
@AutoConfiguration
@EntityScan(basePackageClasses = JobEntity.class)
@EnableJpaRepositories(basePackageClasses = JobRepository.class)
public class JobAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AuditActorResolver.class)
	AuditActorResolver auditActorResolver() {
		return () -> "system";
	}

	@Bean
	JobAuditRecorder jobAuditRecorder(JobAuditEventRepository jobAuditEventRepository) {
		return new JobAuditRecorder(jobAuditEventRepository);
	}

	@Bean
	JobAuditSupport jobAuditSupport(
			JobAuditRecorder jobAuditRecorder,
			AuditActorResolver auditActorResolver) {
		return new JobAuditSupport(jobAuditRecorder, auditActorResolver);
	}

	@Bean
	JobOperations jobOperations(
			JobRepository jobRepository,
			CustomerOperations customerOperations,
			JobAuditEventRepository jobAuditEventRepository,
			JobAuditSupport jobAuditSupport) {
		return new JobApplicationService(
				jobRepository,
				customerOperations,
				jobAuditEventRepository,
				jobAuditSupport);
	}

	@Bean
	JobController jobController(JobOperations jobOperations) {
		return new JobController(jobOperations);
	}
}
