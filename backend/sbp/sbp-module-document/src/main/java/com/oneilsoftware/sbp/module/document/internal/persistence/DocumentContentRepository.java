package com.oneilsoftware.sbp.module.document.internal.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentContentRepository extends JpaRepository<DocumentContentEntity, UUID> {
}
