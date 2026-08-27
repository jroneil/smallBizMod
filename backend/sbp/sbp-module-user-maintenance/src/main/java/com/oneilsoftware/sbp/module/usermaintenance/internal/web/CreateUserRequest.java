package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateUserRequest(
		@NotBlank @Size(max = 100) String username,
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @Size(max = 100) String givenName,
		@NotBlank @Size(max = 100) String familyName,
		@Size(max = 40) String phone) {
}
