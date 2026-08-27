package com.oneilsoftware.sbp.module.document.internal.storage;

import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.framework.storage.BlobStorage;
import com.oneilsoftware.sbp.module.document.internal.application.Timestamps;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentContentEntity;
import com.oneilsoftware.sbp.module.document.internal.persistence.DocumentContentRepository;

/**
 * Reference database-backed {@link BlobStorage} implementation (ADR-DOC-002): content is stored
 * as {@code bytea} in the Document-owned {@code sbp_document_content} table. Registered as the
 * module default and backed off by {@code @ConditionalOnMissingBean}, so a customer deployment can
 * supply its own approved implementation (object store, filesystem, ...) without module changes.
 *
 * <p>Because this implementation writes to the same database and transaction as the Document
 * metadata, content and metadata are stored atomically in the reference composition.
 */
public class DatabaseBlobStorage implements BlobStorage {

	private final DocumentContentRepository repository;

	public DatabaseBlobStorage(DocumentContentRepository repository) {
		this.repository = repository;
	}

	@Override
	public void store(String key, byte[] content, String contentType) {
		repository.save(new DocumentContentEntity(UUID.fromString(key), content, Timestamps.now()));
	}

	@Override
	public Optional<byte[]> load(String key) {
		return repository.findById(UUID.fromString(key)).map(DocumentContentEntity::getContent);
	}

	@Override
	public void delete(String key) {
		repository.findById(UUID.fromString(key)).ifPresent(repository::delete);
	}
}
