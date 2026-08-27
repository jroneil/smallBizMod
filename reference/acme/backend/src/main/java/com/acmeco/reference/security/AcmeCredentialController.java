package com.acmeco.reference.security;

import java.security.Principal;
import java.util.UUID;

import com.acmeco.reference.identity.AcmeIdentityService;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acme-owned credential administration. Self-service changes only the authenticated identity's own
 * credential; administrative reset/unlock targets a selected SBP User. No password value is ever
 * returned.
 */
@RestController
public class AcmeCredentialController {

	private final AcmeCredentialService credentialService;
	private final AcmeIdentityService identityService;

	public AcmeCredentialController(AcmeCredentialService credentialService, AcmeIdentityService identityService) {
		this.credentialService = credentialService;
		this.identityService = identityService;
	}

	@PostMapping("/api/me/password")
	public AcmeCredentialStatus changePassword(
			Principal principal,
			@Valid @RequestBody ChangePasswordRequest request) {
		User current = identityService.currentUser(principal)
				.orElseThrow(() -> new IllegalStateException("No SBP User for authenticated identity"));
		credentialService.changePassword(current.id(), request.currentPassword(), request.newPassword());
		return credentialService.status(current.id());
	}

	@GetMapping("/api/acme/users/{userId}/credential")
	public AcmeCredentialStatus status(@PathVariable UUID userId) {
		return credentialService.status(userId);
	}

	@PutMapping("/api/acme/users/{userId}/password")
	public AcmeCredentialStatus resetPassword(
			@PathVariable UUID userId,
			@Valid @RequestBody ResetPasswordRequest request) {
		credentialService.resetPassword(userId, request.newPassword(), request.requireChange());
		return credentialService.status(userId);
	}

	@PostMapping("/api/acme/users/{userId}/unlock")
	public AcmeCredentialStatus unlock(@PathVariable UUID userId) {
		credentialService.unlock(userId);
		return credentialService.status(userId);
	}

	@ExceptionHandler(InvalidCurrentPasswordException.class)
	ResponseEntity<Void> invalidCurrentPassword() {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

	@ExceptionHandler(InvalidPasswordException.class)
	ResponseEntity<Void> invalidPassword() {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}
}
