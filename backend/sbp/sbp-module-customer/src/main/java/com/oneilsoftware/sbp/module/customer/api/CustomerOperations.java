package com.oneilsoftware.sbp.module.customer.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerOperations {

	Customer create(CreateCustomer command);

	List<Customer> list();

	Optional<Customer> findById(UUID customerId);
}
