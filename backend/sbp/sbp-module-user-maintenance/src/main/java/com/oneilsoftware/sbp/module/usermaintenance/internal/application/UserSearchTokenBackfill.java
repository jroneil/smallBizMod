package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import com.oneilsoftware.sbp.module.usermaintenance.internal.encryption.FieldCipher;
import com.oneilsoftware.sbp.module.usermaintenance.internal.encryption.SearchTokenGenerator;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Backfills blind-index search tokens for encrypted rows created before the search-token columns
 * existed. It decrypts the personal fields (the encryption key is present at startup), computes the
 * tokens with the separate search key, and saves them. Rows that cannot be decrypted are skipped
 * without logging personal data.
 */
public class UserSearchTokenBackfill implements ApplicationRunner {

	private final UserRepository userRepository;
	private final FieldCipher cipher;
	private final SearchTokenGenerator tokenGenerator;

	public UserSearchTokenBackfill(
			UserRepository userRepository,
			FieldCipher cipher,
			SearchTokenGenerator tokenGenerator) {
		this.userRepository = userRepository;
		this.cipher = cipher;
		this.tokenGenerator = tokenGenerator;
	}

	@Override
	public void run(ApplicationArguments args) {
		for (UserEntity entity : userRepository.findByEmailTokenIsNull()) {
			try {
				String email = cipher.decrypt(entity.getEmail());
				String givenName = cipher.decrypt(entity.getGivenName());
				String familyName = cipher.decrypt(entity.getFamilyName());
				entity.updateSearchTokens(
						tokenGenerator.tokenFor(email),
						tokenGenerator.tokenFor(givenName),
						tokenGenerator.tokenFor(familyName),
						tokenGenerator.tokenFor(givenName + " " + familyName));
				userRepository.save(entity);
			}
			catch (RuntimeException ignored) {
				// Pre-encryption plaintext rows are not backfilled; a development reset is expected.
			}
		}
	}
}
