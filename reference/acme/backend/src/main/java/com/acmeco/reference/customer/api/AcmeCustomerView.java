package com.acmeco.reference.customer.api;

import com.oneilsoftware.sbp.module.customer.api.Customer;

public record AcmeCustomerView(
		Customer customer,
		AcmeCustomerProfile profile) {
}
