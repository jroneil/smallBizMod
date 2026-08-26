package com.oneilsoftware.sbp.module.customer.internal.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

	private final CustomerOperations customerOperations;

	public CustomerController(CustomerOperations customerOperations) {
		this.customerOperations = customerOperations;
	}

	@PostMapping
	public ResponseEntity<Customer> create(@Valid @RequestBody CreateCustomerRequest request) {
		Customer customer = customerOperations.create(new CreateCustomer(request.name()));
		return ResponseEntity.created(URI.create("/api/customers/" + customer.id())).body(customer);
	}

	@GetMapping
	public List<Customer> list() {
		return customerOperations.list();
	}

	@GetMapping("/{customerId}")
	public ResponseEntity<Customer> get(@PathVariable UUID customerId) {
		return ResponseEntity.of(customerOperations.findById(customerId));
	}
}
