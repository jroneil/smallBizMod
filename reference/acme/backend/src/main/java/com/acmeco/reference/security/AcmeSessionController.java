package com.acmeco.reference.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/session")
class AcmeSessionController {

	@GetMapping
	AcmeSession currentSession() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Object principal = authentication == null ? null : authentication.getPrincipal();
		boolean passwordChangeRequired =
				principal instanceof AcmeUserDetails userDetails && userDetails.isPasswordChangeRequired();
		return new AcmeSession(authentication.getName(), passwordChangeRequired);
	}

	private record AcmeSession(String username, boolean passwordChangeRequired) {
	}
}
