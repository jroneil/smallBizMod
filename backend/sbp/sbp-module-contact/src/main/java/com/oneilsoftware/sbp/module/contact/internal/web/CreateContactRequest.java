package com.oneilsoftware.sbp.module.contact.internal.web;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record CreateContactRequest(
		@NotNull UUID customerId,
		@NotBlank @Size(max = 200) String fullName,
		@Email @Size(max = 200) String email,
		@Size(max = 40) String phone,
		@Size(max = 100) String roleTitle) {
}
