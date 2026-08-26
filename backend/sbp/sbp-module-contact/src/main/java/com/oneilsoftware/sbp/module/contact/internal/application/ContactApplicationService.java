package com.oneilsoftware.sbp.module.contact.internal.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.module.contact.api.Contact;
import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.api.ContactStatus;
import com.oneilsoftware.sbp.module.contact.api.CreateContact;
import com.oneilsoftware.sbp.module.contact.internal.persistence.ContactEntity;
import com.oneilsoftware.sbp.module.contact.internal.persistence.ContactRepository;
import org.springframework.transaction.annotation.Transactional;

public class ContactApplicationService implements ContactOperations {

	private final ContactRepository contactRepository;

	public ContactApplicationService(ContactRepository contactRepository) {
		this.contactRepository = contactRepository;
	}

	@Override
	@Transactional
	public Contact create(CreateContact command) {
		ContactEntity contact = new ContactEntity(
				UUID.randomUUID(),
				command.customerId(),
				command.fullName(),
				command.role(),
				command.email(),
				command.phone(),
				ContactStatus.ACTIVE);

		return toContact(contactRepository.save(contact));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Contact> listByCustomer(UUID customerId) {
		return contactRepository.findByCustomerIdOrderByFullNameAsc(customerId).stream()
				.map(ContactApplicationService::toContact)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Contact> list() {
		return contactRepository.findAllByOrderByFullNameAsc().stream()
				.map(ContactApplicationService::toContact)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Contact> findById(UUID contactId) {
		return contactRepository.findById(contactId)
				.map(ContactApplicationService::toContact);
	}

	private static Contact toContact(ContactEntity entity) {
		return new Contact(
				entity.getId(),
				entity.getCustomerId(),
				entity.getFullName(),
				entity.getRole(),
				entity.getEmail(),
				entity.getPhone(),
				entity.getStatus());
	}
}
