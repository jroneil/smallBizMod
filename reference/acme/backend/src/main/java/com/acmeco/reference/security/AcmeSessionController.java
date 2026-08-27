package com.acmeco.reference.security;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/session")
class AcmeSessionController {

	@GetMapping
	AcmeSession currentSession(Principal principal) {
		return new AcmeSession(principal.getName());
	}

	private record AcmeSession(String username) {
	}
}
