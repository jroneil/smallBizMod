package com.oneilsoftware.sbp.module.document.internal.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.framework.storage.BlobStorage;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.document.api.CreateDocument;
import com.oneilsoftware.sbp.module.document.api.Document;
import com.oneilsoftware.sbp.module.document.api.DocumentAuditActions;
import com.oneilsoftware.sbp.module.document.api.DocumentAuditEvent;
import com.oneilsoftware.sbp.module.document.api.DocumentContent;
import com.oneilsoftware.sbp.module.document.api.DocumentFilter;
import com.oneilsoftware.sbp.module.document.api.DocumentOperations;
import com.oneilsoftware.sbp.module.document.api.DocumentOwnerType;
import com.oneilsoftware.sbp.module.document.api.DocumentPage;
import com.oneilsoftware.sbp.module.document.api.DocumentSizeLimitExceededException;
import com.oneilsoftware.sbp.module.document.api.ReplaceDocumentContent;
import com.oneilsoftware.sbp.module.document.api.UnknownDocumentException;
import com.oneilsoftware.sbp.module.document.api.UnknownDocumentOwnerException;
import com.oneilsoftware.sbp.module.document.api.UpdateDocumentMetadata;
import com.oneilsoftware.sbp.module.document.internal.DocumentProperties;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentAuditEventEntity;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentAuditEventRepository;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentEntity;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentRepository;
import com.oneilsoftware.sbp.module.job.api.JobOperations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing the Document public contract. Owner references are validated
 * only through {@link CustomerOperations} and {@link JobOperations}; the Document module never
 * touches Customer or Job persistence internals (DOC-FR-07, ADR-DOC-001). Content is stored and
 * retrieved only through the framework {@link BlobStorage} abstraction (DOC-FR-08, ADR-DOC-002).
 * Every upload, rename, content replacement, and deletion is audited in the same transaction
 * through the framework audit hook (DOC-FR-09, ADR-DOC-004).
 */
public class DocumentApplicationService implements DocumentOperations {

	private final DocumentRepository documentRepository;
	private final CustomerOperations customerOperations;
	private final JobOperations jobOperations;
	private final BlobStorage blobStorage;
	private final DocumentAuditEventRepository documentAuditEventRepository;
	private final DocumentAuditSupport auditSupport;
	private final DocumentProperties properties;

	public DocumentApplicationService(
			DocumentRepository documentRepository,
			CustomerOperations customerOperations,
			JobOperations jobOperations,
			BlobStorage blobStorage,
			DocumentAuditEventRepository documentAuditEventRepository,
			DocumentAuditSupport auditSupport,
			DocumentProperties properties) {
		this.documentRepository = documentRepository;
		this.customerOperations = customerOperations;
		this.jobOperations = jobOperations;
		this.blobStorage = blobStorage;
		this.documentAuditEventRepository = documentAuditEventRepository;
		this.auditSupport = auditSupport;
		this.properties = properties;
	}

	@Override
	@Transactional
	public Document create(CreateDocument command) {
		validateOwner(command.ownerType(), command.ownerId());
		enforceSizeLimit(command.content().length);

		Instant now = Timestamps.now();
		UUID id = UUID.randomUUID();
		blobStorage.store(id.toString(), command.content(), command.contentType());
		try {
			DocumentEntity entity = new DocumentEntity(
					id,
					command.ownerType() == DocumentOwnerType.CUSTOMER ? command.ownerId() : null,
					command.ownerType() == DocumentOwnerType.JOB ? command.ownerId() : null,
					command.displayName(),
					command.contentType(),
					command.content().length,
					now,
					now);
			Document saved = toDocument(documentRepository.save(entity));
			auditSupport.record(
					DocumentAuditActions.SUBJECT_DOCUMENT,
					saved.id().toString(),
					DocumentAuditActions.DOCUMENT_CREATED,
					List.of("ownerType", "ownerId", "displayName", "contentType", "sizeBytes"),
					null);
			return saved;
		} catch (RuntimeException failure) {
			// Best-effort cleanup for non-transactional storage implementations; with the reference
			// database-backed implementation the whole transaction (including the content row)
			// rolls back together (ADR-DOC-002).
			blobStorage.delete(id.toString());
			throw failure;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public DocumentPage search(DocumentFilter filter) {
		UUID customerId = filter.ownerType() == DocumentOwnerType.CUSTOMER ? filter.ownerId() : null;
		UUID jobId = filter.ownerType() == DocumentOwnerType.JOB ? filter.ownerId() : null;
		Page<DocumentEntity> page = documentRepository.search(
				filter.query(),
				customerId,
				jobId,
				PageRequest.of(filter.page(), filter.size()));
		return new DocumentPage(
				page.getContent().stream().map(DocumentApplicationService::toDocument).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Document> findById(UUID documentId) {
		return documentRepository.findById(documentId).map(DocumentApplicationService::toDocument);
	}

	@Override
	@Transactional
	public Document updateDisplayName(UUID documentId, UpdateDocumentMetadata command) {
		DocumentEntity entity = requireDocument(documentId);
		List<String> changed = new ArrayList<>();
		if (!Objects.equals(entity.getDisplayName(), command.displayName())) {
			changed.add("displayName");
		}
		entity.rename(command.displayName(), Timestamps.now());
		Document saved = toDocument(documentRepository.save(entity));
		auditSupport.record(
				DocumentAuditActions.SUBJECT_DOCUMENT,
				saved.id().toString(),
				DocumentAuditActions.DOCUMENT_METADATA_UPDATED,
				changed,
				null);
		return saved;
	}

	@Override
	@Transactional
	public Document replaceContent(UUID documentId, ReplaceDocumentContent command) {
		DocumentEntity entity = requireDocument(documentId);
		enforceSizeLimit(command.content().length);

		blobStorage.store(documentId.toString(), command.content(), command.contentType());
		entity.replaceContent(command.contentType(), command.content().length, Timestamps.now());
		Document saved = toDocument(documentRepository.save(entity));
		auditSupport.record(
				DocumentAuditActions.SUBJECT_DOCUMENT,
				saved.id().toString(),
				DocumentAuditActions.DOCUMENT_CONTENT_REPLACED,
				List.of("content", "contentType", "sizeBytes"),
				null);
		return saved;
	}

	@Override
	@Transactional
	public void delete(UUID documentId) {
		DocumentEntity entity = requireDocument(documentId);
		documentRepository.delete(entity);
		blobStorage.delete(documentId.toString());
		auditSupport.record(
				DocumentAuditActions.SUBJECT_DOCUMENT,
				documentId.toString(),
				DocumentAuditActions.DOCUMENT_DELETED,
				List.of(),
				null);
	}

	@Override
	@Transactional(readOnly = true)
	public DocumentContent download(UUID documentId) {
		DocumentEntity entity = requireDocument(documentId);
		byte[] content = blobStorage.load(documentId.toString())
				.orElseThrow(() -> new IllegalStateException(
						"Stored content missing for document " + documentId));
		return new DocumentContent(content, entity.getContentType(), entity.getDisplayName());
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentAuditEvent> listAuditEvents(UUID documentId, String action) {
		// No requireDocument here: audit history must remain readable after the Document is deleted
		// (DOC-FR-06), when the metadata row no longer exists.
		return documentAuditEventRepository.findBySubjectId(documentId.toString(), trimToNull(action)).stream()
				.map(DocumentApplicationService::toAuditEvent)
				.toList();
	}

	private void validateOwner(DocumentOwnerType ownerType, UUID ownerId) {
		boolean exists = switch (ownerType) {
			case CUSTOMER -> customerOperations.findById(ownerId).isPresent();
			case JOB -> jobOperations.findById(ownerId).isPresent();
		};
		if (!exists) {
			throw new UnknownDocumentOwnerException(ownerType, ownerId);
		}
	}

	private void enforceSizeLimit(long actualBytes) {
		if (actualBytes > properties.maxSizeBytes()) {
			throw new DocumentSizeLimitExceededException(properties.maxSizeBytes(), actualBytes);
		}
	}

	private DocumentEntity requireDocument(UUID documentId) {
		return documentRepository.findById(documentId)
				.orElseThrow(() -> new UnknownDocumentException(documentId));
	}

	private static DocumentAuditEvent toAuditEvent(DocumentAuditEventEntity entity) {
		return new DocumentAuditEvent(
				entity.getId(),
				entity.getActor(),
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

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static Document toDocument(DocumentEntity entity) {
		boolean customerOwner = entity.getCustomerId() != null;
		return new Document(
				entity.getId(),
				customerOwner ? DocumentOwnerType.CUSTOMER : DocumentOwnerType.JOB,
				customerOwner ? entity.getCustomerId() : entity.getJobId(),
				entity.getDisplayName(),
				entity.getContentType(),
				entity.getSizeBytes(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
