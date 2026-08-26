package com.acmeco.reference.customer.internal.application;

import java.util.Optional;
import java.util.UUID;

import com.acmeco.reference.customer.api.AcmeCustomerProfile;
import com.acmeco.reference.customer.api.AcmeCustomerView;
import com.acmeco.reference.customer.api.SaveAcmeCustomerProfile;
import com.acmeco.reference.customer.internal.persistence.AcmeCustomerProfileEntity;
import com.acmeco.reference.customer.internal.persistence.AcmeCustomerProfileRepository;
import com.oneilsoftware.sbp.module.customer.api.Customer;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcmeCustomerProfileService {

	private final CustomerOperations customerOperations;
	private final AcmeCustomerProfileRepository profileRepository;

	public AcmeCustomerProfileService(
			CustomerOperations customerOperations,
			AcmeCustomerProfileRepository profileRepository) {
		this.customerOperations = customerOperations;
		this.profileRepository = profileRepository;
	}

	@Transactional
	public Optional<AcmeCustomerView> save(
			UUID customerId,
			SaveAcmeCustomerProfile command) {
		return customerOperations.findById(customerId)
				.map(customer -> save(customer, command));
	}

	@Transactional(readOnly = true)
	public Optional<AcmeCustomerView> findByCustomerId(UUID customerId) {
		return customerOperations.findById(customerId)
				.flatMap(customer -> profileRepository.findById(customerId)
						.map(profile -> toView(customer, profile)));
	}

	private AcmeCustomerView save(Customer customer, SaveAcmeCustomerProfile command) {
		AcmeCustomerProfileEntity profile = profileRepository.findById(customer.id())
				.orElseGet(() -> new AcmeCustomerProfileEntity(customer.id()));
		profile.update(command.serviceRegion(), command.accountNotes());
		return toView(customer, profileRepository.save(profile));
	}

	private static AcmeCustomerView toView(
			Customer customer,
			AcmeCustomerProfileEntity entity) {
		AcmeCustomerProfile profile = new AcmeCustomerProfile(
				entity.getCustomerId(),
				entity.getServiceRegion(),
				entity.getAccountNotes());
		return new AcmeCustomerView(customer, profile);
	}
}
