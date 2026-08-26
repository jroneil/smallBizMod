package com.acmeco.reference.customer.internal.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcmeCustomerProfileRepository extends JpaRepository<AcmeCustomerProfileEntity, UUID> {
}
