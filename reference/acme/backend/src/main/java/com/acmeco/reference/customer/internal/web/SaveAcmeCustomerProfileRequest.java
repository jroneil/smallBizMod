package com.acmeco.reference.customer.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record SaveAcmeCustomerProfileRequest(
		@NotBlank @Size(max = 100) String serviceRegion,
		@Size(max = 2000) String accountNotes) {
}
