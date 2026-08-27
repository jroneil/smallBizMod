package com.oneilsoftware.sbp.module.contact.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactOperations {

	Contact create(CreateContact command);

	Optional<Contact> findById(UUID contactId);

	List<Contact> listByCustomer(UUID customerId);
}
