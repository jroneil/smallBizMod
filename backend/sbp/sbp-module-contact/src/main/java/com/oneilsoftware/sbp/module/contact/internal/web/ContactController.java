package com.oneilsoftware.sbp.module.contact.internal.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.contact.api.Contact;
import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.api.CreateContact;
import com.oneilsoftware.sbp.module.contact.api.UnknownCustomerException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactController {

	private final ContactOperations contactOperations;

	public ContactController(ContactOperations contactOperations) {
		this.contactOperations = contactOperations;
	}

	@PostMapping("/api/contacts")
	public ResponseEntity<Contact> create(@Valid @RequestBody CreateContactRequest request) {
		Contact contact = contactOperations.create(new CreateContact(
				request.customerId(),
				request.fullName(),
				request.email(),
				request.phone(),
				request.roleTitle()));
		return ResponseEntity.created(URI.create("/api/contacts/" + contact.id())).body(contact);
	}

	@GetMapping("/api/contacts/{contactId}")
	public ResponseEntity<Contact> get(@PathVariable UUID contactId) {
		return ResponseEntity.of(contactOperations.findById(contactId));
	}

	@GetMapping("/api/customers/{customerId}/contacts")
	public List<Contact> listByCustomer(@PathVariable UUID customerId) {
		return contactOperations.listByCustomer(customerId);
	}

	@ExceptionHandler(UnknownCustomerException.class)
	ResponseEntity<Void> unknownCustomer() {
		return ResponseEntity.notFound().build();
	}
}
