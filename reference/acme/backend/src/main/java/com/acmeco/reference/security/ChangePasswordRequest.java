package com.acmeco.reference.security;

import jakarta.validation.constraints.NotBlank;

record ChangePasswordRequest(
		@NotBlank String currentPassword,
		@NotBlank String newPassword) {
}
