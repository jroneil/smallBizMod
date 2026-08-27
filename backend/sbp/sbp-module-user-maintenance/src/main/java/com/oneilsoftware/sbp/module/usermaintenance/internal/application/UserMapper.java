package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.internal.encryption.FieldCipher;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserEntity;

/**
 * Maps a persisted {@link UserEntity} to the public {@link User} record, decrypting the personal
 * fields on read so the public contract always carries plaintext while the database holds
 * ciphertext.
 */
public final class UserMapper {

	private final FieldCipher cipher;

	public UserMapper(FieldCipher cipher) {
		this.cipher = cipher;
	}

	public User toUser(UserEntity entity) {
		return new User(
				entity.getId(),
				entity.getUsername(),
				cipher.decrypt(entity.getEmail()),
				cipher.decrypt(entity.getGivenName()),
				cipher.decrypt(entity.getFamilyName()),
				cipher.decrypt(entity.getPhone()),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
