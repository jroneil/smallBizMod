package com.oneilsoftware.sbp.module.contact.internal.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.contact.api.Contact;
import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.api.CreateContact;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

	private final ContactOperations contactOperations;

	public ContactController(ContactOperations contactOperations) {
		this.contactOperations = contactOperations;
	}

	@PostMapping
	public ResponseEntity<Contact> create(@Valid @RequestBody CreateContactRequest request) {
		Contact contact = contactOperations.create(new CreateContact(
				request.customerId(),
				request.fullName(),
				request.role(),
				request.email(),
				request.phone()));
		return ResponseEntity.created(URI.create("/api/contacts/" + contact.id())).body(contact);
	}

	@GetMapping
	public List<Contact> list(@RequestParam(required = false) UUID customerId) {
		return customerId == null
				? contactOperations.list()
				: contactOperations.listByCustomer(customerId);
	}

	@GetMapping("/{contactId}")
	public ResponseEntity<Contact> get(@PathVariable UUID contactId) {
		return ResponseEntity.of(contactOperations.findById(contactId));
	}
}
