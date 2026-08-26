package com.acmeco.reference.customer.internal.web;

import java.util.UUID;

import com.acmeco.reference.customer.api.AcmeCustomerView;
import com.acmeco.reference.customer.api.SaveAcmeCustomerProfile;
import com.acmeco.reference.customer.internal.application.AcmeCustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/acme/customers")
public class AcmeCustomerController {

	private final AcmeCustomerProfileService profileService;

	public AcmeCustomerController(AcmeCustomerProfileService profileService) {
		this.profileService = profileService;
	}

	@PutMapping("/{customerId}/profile")
	public ResponseEntity<AcmeCustomerView> saveProfile(
			@PathVariable UUID customerId,
			@Valid @RequestBody SaveAcmeCustomerProfileRequest request) {
		SaveAcmeCustomerProfile command = new SaveAcmeCustomerProfile(
				request.serviceRegion(),
				request.accountNotes());
		return ResponseEntity.of(profileService.save(customerId, command));
	}

	@GetMapping("/{customerId}")
	public ResponseEntity<AcmeCustomerView> get(@PathVariable UUID customerId) {
		return ResponseEntity.of(profileService.findByCustomerId(customerId));
	}
}
