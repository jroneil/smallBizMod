package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * User Maintenance-owned persistence mapping. Personal fields (email, given name, family name,
 * phone) hold AES-256-GCM ciphertext at rest and are decrypted only at the application boundary.
 * {@code username} is application identity and remains plaintext.
 */
@Entity
@Table(name = "sbp_user")
public class UserEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 100)
	private String username;

	@Column(nullable = false, length = 500)
	private String email;

	@Column(name = "given_name", nullable = false, length = 250)
	private String givenName;

	@Column(name = "family_name", nullable = false, length = 250)
	private String familyName;

	@Column(length = 150)
	private String phone;

	@Column(name = "email_token", length = 64)
	private String emailToken;

	@Column(name = "given_name_token", length = 64)
	private String givenNameToken;

	@Column(name = "family_name_token", length = 64)
	private String familyNameToken;

	@Column(name = "full_name_token", length = 64)
	private String fullNameToken;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserEntity() {
	}

	public UserEntity(
			UUID id,
			String username,
			String email,
			String givenName,
			String familyName,
			String phone,
			UserStatus status,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.givenName = givenName;
		this.familyName = familyName;
		this.phone = phone;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void updateProfile(
			String email,
			String givenName,
			String familyName,
			String phone,
			Instant updatedAt) {
		this.email = email;
		this.givenName = givenName;
		this.familyName = familyName;
		this.phone = phone;
		this.updatedAt = updatedAt;
	}

	public void changeStatus(UserStatus status, Instant updatedAt) {
		this.status = status;
		this.updatedAt = updatedAt;
	}

	public void updateSearchTokens(
			String emailToken,
			String givenNameToken,
			String familyNameToken,
			String fullNameToken) {
		this.emailToken = emailToken;
		this.givenNameToken = givenNameToken;
		this.familyNameToken = familyNameToken;
		this.fullNameToken = fullNameToken;
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getGivenName() {
		return givenName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmailToken() {
		return emailToken;
	}

	public String getGivenNameToken() {
		return givenNameToken;
	}

	public String getFamilyNameToken() {
		return familyNameToken;
	}

	public String getFullNameToken() {
		return fullNameToken;
	}

	public UserStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
