package com.oneilsoftware.sbp.module.customer.internal.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.module.customer.api.CreateCustomer;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.customer.api.CustomerStatus;
import com.oneilsoftware.sbp.module.customer.internal.persistence.CustomerEntity;
import com.oneilsoftware.sbp.module.customer.internal.persistence.CustomerRepository;
import org.springframework.transaction.annotation.Transactional;

public class CustomerApplicationService implements CustomerOperations {

	private final CustomerRepository customerRepository;

	public CustomerApplicationService(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	@Override
	@Transactional
	public Customer create(CreateCustomer command) {
		CustomerEntity customer = new CustomerEntity(
				UUID.randomUUID(),
				command.name(),
				CustomerStatus.ACTIVE);

		return toCustomer(customerRepository.save(customer));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Customer> list() {
		return customerRepository.findAll().stream()
				.map(CustomerApplicationService::toCustomer)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Customer> findById(UUID customerId) {
		return customerRepository.findById(customerId)
				.map(CustomerApplicationService::toCustomer);
	}

	private static Customer toCustomer(CustomerEntity entity) {
		return new Customer(entity.getId(), entity.getName(), entity.getStatus());
	}
}
