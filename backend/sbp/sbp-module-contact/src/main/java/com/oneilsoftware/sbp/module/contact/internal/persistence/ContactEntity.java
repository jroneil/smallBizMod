package com.oneilsoftware.sbp.module.contact.internal.persistence;

import java.util.UUID;

import com.oneilsoftware.sbp.module.contact.api.ContactStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sbp_contact")
public class ContactEntity {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(name = "full_name", nullable = false, length = 200)
	private String fullName;

	@Column(name = "role", length = 100)
	private String role;

	@Column(name = "email", length = 200)
	private String email;

	@Column(name = "phone", length = 40)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContactStatus status;

	protected ContactEntity() {
	}

	public ContactEntity(
			UUID id,
			UUID customerId,
			String fullName,
			String role,
			String email,
			String phone,
			ContactStatus status) {
		this.id = id;
		this.customerId = customerId;
		this.fullName = fullName;
		this.role = role;
		this.email = email;
		this.phone = phone;
		this.status = status;
	}

	public UUID getId() {
		return id;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public String getFullName() {
		return fullName;
	}

	public String getRole() {
		return role;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public ContactStatus getStatus() {
		return status;
	}
}
