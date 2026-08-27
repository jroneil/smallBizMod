package com.acmeco.reference.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Customer-owned self-service profile payload. It deliberately exposes only non-privileged fields:
 * username, lifecycle status, roles, and groups are not editable through self-service.
 */
record UpdateMyProfileRequest(
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @Size(max = 100) String givenName,
		@NotBlank @Size(max = 100) String familyName,
		@Size(max = 40) String phone) {
}
