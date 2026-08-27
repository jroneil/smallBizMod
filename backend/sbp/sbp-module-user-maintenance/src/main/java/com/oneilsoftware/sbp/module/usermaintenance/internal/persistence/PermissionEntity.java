package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sbp_permission")
public class PermissionEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 200)
	private String key;

	@Column(nullable = false, length = 500)
	private String description;

	protected PermissionEntity() {
	}

	public PermissionEntity(UUID id, String key, String description) {
		this.id = id;
		this.key = key;
		this.description = description;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public UUID getId() {
		return id;
	}

	public String getKey() {
		return key;
	}

	public String getDescription() {
		return description;
	}
}
