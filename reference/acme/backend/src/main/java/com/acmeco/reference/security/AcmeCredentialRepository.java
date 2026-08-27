package com.acmeco.reference.security;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcmeCredentialRepository extends JpaRepository<AcmeCredentialEntity, UUID> {
}
