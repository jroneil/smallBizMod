package com.acmeco.reference.identity;

import java.security.Principal;

import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserOperations;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-owned self-service profile endpoint. It resolves the authenticated identity through
 * Acme's mapping and edits only the permitted non-privileged User fields via the reusable public
 * contract. Authentication and authorization remain Acme's responsibility.
 */
@RestController
@RequestMapping("/api/me")
public class AcmeProfileController {

	private final AcmeIdentityService identityService;
	private final UserOperations userOperations;

	public AcmeProfileController(AcmeIdentityService identityService, UserOperations userOperations) {
		this.identityService = identityService;
		this.userOperations = userOperations;
	}

	@GetMapping
	public ResponseEntity<User> me(Principal principal) {
		return ResponseEntity.of(identityService.currentUser(principal));
	}

	@PutMapping
	public ResponseEntity<User> updateMe(
			Principal principal,
			@Valid @RequestBody UpdateMyProfileRequest request) {
		return identityService.currentUser(principal)
				.map(current -> userOperations.update(current.id(), new UpdateUser(
						request.email(),
						request.givenName(),
						request.familyName(),
						request.phone())))
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
