package com.acmeco.reference.customer.internal.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "acme_customer_profile")
public class AcmeCustomerProfileEntity {

	@Id
	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(name = "service_region", nullable = false, length = 100)
	private String serviceRegion;

	@Column(name = "account_notes", length = 2000)
	private String accountNotes;

	protected AcmeCustomerProfileEntity() {
	}

	public AcmeCustomerProfileEntity(UUID customerId) {
		this.customerId = customerId;
	}

	public void update(String serviceRegion, String accountNotes) {
		this.serviceRegion = serviceRegion;
		this.accountNotes = accountNotes;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public String getServiceRegion() {
		return serviceRegion;
	}

	public String getAccountNotes() {
		return accountNotes;
	}
}
