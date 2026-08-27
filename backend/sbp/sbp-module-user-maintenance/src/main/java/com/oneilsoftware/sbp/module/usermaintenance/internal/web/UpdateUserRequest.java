package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateUserRequest(
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @Size(max = 100) String givenName,
		@NotBlank @Size(max = 100) String familyName,
		@Size(max = 40) String phone) {
}
