package com.acmeco.reference.security;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Acme-owned local credential record. Stores only a one-way password hash and failed-attempt
 * accounting, keyed by the stable SBP User id. It never stores a reversible password and lives
 * outside the reusable User Maintenance profile tables.
 */
@Entity
@Table(name = "acme_user_credential")
public class AcmeCredentialEntity {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "password_hash", nullable = false, length = 200)
	private String passwordHash;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "password_change_required", nullable = false)
	private boolean passwordChangeRequired;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AcmeCredentialEntity() {
	}

	public AcmeCredentialEntity(UUID userId, String passwordHash, int failedAttempts, Instant updatedAt) {
		this.userId = userId;
		this.passwordHash = passwordHash;
		this.failedAttempts = failedAttempts;
		this.passwordChangeRequired = false;
		this.updatedAt = updatedAt;
	}

	public void setPassword(String passwordHash, Instant updatedAt) {
		this.passwordHash = passwordHash;
		this.failedAttempts = 0;
		this.passwordChangeRequired = false;
		this.updatedAt = updatedAt;
	}

	public void setPasswordChangeRequired(boolean required, Instant updatedAt) {
		this.passwordChangeRequired = required;
		this.updatedAt = updatedAt;
	}

	public void recordFailure(Instant updatedAt) {
		this.failedAttempts++;
		this.updatedAt = updatedAt;
	}

	public void resetFailures(Instant updatedAt) {
		this.failedAttempts = 0;
		this.updatedAt = updatedAt;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public boolean isPasswordChangeRequired() {
		return passwordChangeRequired;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
