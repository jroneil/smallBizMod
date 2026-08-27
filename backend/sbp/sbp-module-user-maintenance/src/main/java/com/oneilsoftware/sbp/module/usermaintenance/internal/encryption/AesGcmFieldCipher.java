package com.oneilsoftware.sbp.module.usermaintenance.internal.encryption;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM field cipher. Each encryption uses a fresh random 12-byte IV; the stored value is
 * Base64(IV || ciphertext||tag). A null input returns null so optional fields stay optional.
 */
public final class AesGcmFieldCipher implements FieldCipher {

	private static final String CIPHER = "AES/GCM/NoPadding";
	private static final int IV_LENGTH_BYTES = 12;
	private static final int TAG_LENGTH_BITS = 128;
	private static final int KEY_LENGTH_BYTES = 32;

	private final SecretKey key;
	private final SecureRandom secureRandom = new SecureRandom();

	public AesGcmFieldCipher(String base64Key) {
		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(base64Key);
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("sbp.usermaintenance.encryption-key must be Base64-encoded", exception);
		}
		if (keyBytes.length != KEY_LENGTH_BYTES) {
			throw new IllegalArgumentException("sbp.usermaintenance.encryption-key must decode to 256 bits");
		}
		this.key = new SecretKeySpec(keyBytes, "AES");
	}

	@Override
	public String encrypt(String plaintext) {
		if (plaintext == null) {
			return null;
		}
		byte[] iv = new byte[IV_LENGTH_BYTES];
		secureRandom.nextBytes(iv);
		try {
			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			byte[] combined = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(combined);
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to encrypt field", exception);
		}
	}

	@Override
	public String decrypt(String ciphertext) {
		if (ciphertext == null) {
			return null;
		}
		byte[] combined = Base64.getDecoder().decode(ciphertext);
		byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
		byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);
		try {
			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to decrypt field", exception);
		}
	}
}
