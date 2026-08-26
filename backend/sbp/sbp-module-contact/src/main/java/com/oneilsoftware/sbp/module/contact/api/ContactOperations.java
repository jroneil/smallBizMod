package com.oneilsoftware.sbp.module.contact.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactOperations {

	Contact create(CreateContact command);

	List<Contact> listByCustomer(UUID customerId);

	List<Contact> list();

	Optional<Contact> findById(UUID contactId);
}
