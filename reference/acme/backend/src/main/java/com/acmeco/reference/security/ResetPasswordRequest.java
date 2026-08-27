package com.acmeco.reference.security;

import jakarta.validation.constraints.NotBlank;

record ResetPasswordRequest(
		@NotBlank String newPassword,
		boolean requireChange) {
}
