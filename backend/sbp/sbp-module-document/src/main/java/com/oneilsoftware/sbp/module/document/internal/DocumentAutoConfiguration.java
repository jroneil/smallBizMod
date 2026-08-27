package com.oneilsoftware.sbp.module.document.internal;

import com.oneilsoftware.sbp.framework.audit.AuditActorResolver;
import com.oneilsoftware.sbp.framework.storage.BlobStorage;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.document.api.DocumentOperations;
import com.oneilsoftware.sbp.module.document.internal.application.DocumentApplicationService;
import com.oneilsoftware.sbp.module.document.internal.application.DocumentAuditRecorder;
import com.oneilsoftware.sbp.module.document.internal.application.DocumentAuditSupport;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentAuditEventRepository;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentContentRepository;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentEntity;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentRepository;
import com.oneilsoftware.sbp.module.document.internal.storage.DatabaseBlobStorage;
import com.oneilsoftware.sbp.module.document.internal.web.DocumentController;
import com.oneilsoftware.sbp.module.job.api.JobOperations;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Activates the Document capability when the {@code sbp-module-document} JAR is on the classpath
 * (ADR-002). Owner validation happens through the public {@link CustomerOperations} and
 * {@link JobOperations} contracts; the audit recorder is exposed under its concrete type
 * {@link DocumentAuditRecorder} so it never competes with User Maintenance's or Job's
 * {@code AuditRecorder} beans; the framework audit hook contract is still implemented
 * (ADR-DOC-004). The default {@code AuditActorResolver} backs off when the customer application
 * supplies its own resolver.
 *
 * <p>This configuration is ordered before Spring Boot's {@link MultipartAutoConfiguration} so the
 * module's own {@code sbp.document.max-size-bytes} is the effective web-layer upload cap out of
 * the box (NFR-DOC-07): the module's {@link MultipartConfigElement} bean registers first and Boot's
 * property-based bean backs off, which means {@code spring.servlet.multipart.*} properties are
 * superseded by the module limit while the module's bean is active. A deployment that defines its
 * own {@link MultipartConfigElement} bean (or a customer-supplied web configuration) takes over.
 */
@AutoConfiguration
@AutoConfigureBefore(MultipartAutoConfiguration.class)
@EnableConfigurationProperties(DocumentProperties.class)
@EntityScan(basePackageClasses = DocumentEntity.class)
@EnableJpaRepositories(basePackageClasses = DocumentRepository.class)
public class DocumentAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AuditActorResolver.class)
	AuditActorResolver auditActorResolver() {
		return () -> "system";
	}

	@Bean
	@ConditionalOnMissingBean(BlobStorage.class)
	BlobStorage blobStorage(DocumentContentRepository documentContentRepository) {
		return new DatabaseBlobStorage(documentContentRepository);
	}

	@Bean
	@ConditionalOnMissingBean(MultipartConfigElement.class)
	MultipartConfigElement multipartConfigElement(DocumentProperties properties) {
		long limit = properties.maxSizeBytes();
		return new MultipartConfigElement("", limit, limit, 0);
	}

	@Bean
	DocumentAuditRecorder documentAuditRecorder(DocumentAuditEventRepository documentAuditEventRepository) {
		return new DocumentAuditRecorder(documentAuditEventRepository);
	}

	@Bean
	DocumentAuditSupport documentAuditSupport(
			DocumentAuditRecorder documentAuditRecorder,
			AuditActorResolver auditActorResolver) {
		return new DocumentAuditSupport(documentAuditRecorder, auditActorResolver);
	}

	@Bean
	DocumentOperations documentOperations(
			DocumentRepository documentRepository,
			CustomerOperations customerOperations,
			JobOperations jobOperations,
			BlobStorage blobStorage,
			DocumentAuditEventRepository documentAuditEventRepository,
			DocumentAuditSupport documentAuditSupport,
			DocumentProperties documentProperties) {
		return new DocumentApplicationService(
				documentRepository,
				customerOperations,
				jobOperations,
				blobStorage,
				documentAuditEventRepository,
				documentAuditSupport,
				documentProperties);
	}

	@Bean
	DocumentController documentController(
			DocumentOperations documentOperations,
			DocumentProperties documentProperties) {
		return new DocumentController(documentOperations, documentProperties);
	}
}
