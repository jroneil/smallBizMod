package com.oneilsoftware.sbp.module.contact.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<ContactEntity, UUID> {

	List<ContactEntity> findByCustomerIdOrderByFullNameAsc(UUID customerId);
}
