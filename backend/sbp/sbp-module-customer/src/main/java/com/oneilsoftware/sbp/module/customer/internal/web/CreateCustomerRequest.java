package com.oneilsoftware.sbp.module.customer.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateCustomerRequest(
		@NotBlank @Size(max = 200) String name) {
}
