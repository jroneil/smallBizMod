package com.oneilsoftware.sbp.module.customer.internal.persistence;

import java.util.UUID;

import com.oneilsoftware.sbp.module.customer.api.CustomerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sbp_customer")
public class CustomerEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CustomerStatus status;

	protected CustomerEntity() {
	}

	public CustomerEntity(UUID id, String name, CustomerStatus status) {
		this.id = id;
		this.name = name;
		this.status = status;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public CustomerStatus getStatus() {
		return status;
	}
}
