package com.oneilsoftware.sbp.module.contact.internal.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.module.contact.api.Contact;
import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.api.ContactStatus;
import com.oneilsoftware.sbp.module.contact.api.CreateContact;
import com.oneilsoftware.sbp.module.contact.api.UnknownCustomerException;
import com.oneilsoftware.sbp.module.contact.internal.persistence.ContactEntity;
import com.oneilsoftware.sbp.module.contact.internal.persistence.ContactRepository;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import org.springframework.transaction.annotation.Transactional;

public class ContactApplicationService implements ContactOperations {

	private final ContactRepository contactRepository;
	private final CustomerOperations customerOperations;

	public ContactApplicationService(
			ContactRepository contactRepository,
			CustomerOperations customerOperations) {
		this.contactRepository = contactRepository;
		this.customerOperations = customerOperations;
	}

	@Override
	@Transactional
	public Contact create(CreateContact command) {
		if (customerOperations.findById(command.customerId()).isEmpty()) {
			throw new UnknownCustomerException(command.customerId());
		}

		ContactEntity contact = new ContactEntity(
				UUID.randomUUID(),
				command.customerId(),
				command.fullName(),
				command.email(),
				command.phone(),
				command.roleTitle(),
				ContactStatus.ACTIVE);

		return toContact(contactRepository.save(contact));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Contact> findById(UUID contactId) {
		return contactRepository.findById(contactId)
				.map(ContactApplicationService::toContact);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Contact> listByCustomer(UUID customerId) {
		return contactRepository.findByCustomerIdOrderByFullNameAsc(customerId).stream()
				.map(ContactApplicationService::toContact)
				.toList();
	}

	private static Contact toContact(ContactEntity entity) {
		return new Contact(
				entity.getId(),
				entity.getCustomerId(),
				entity.getFullName(),
				entity.getEmail(),
				entity.getPhone(),
				entity.getRoleTitle(),
				entity.getStatus());
	}
}
