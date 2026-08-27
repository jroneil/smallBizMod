package com.oneilsoftware.sbp.module.usermaintenance.internal.encryption;

/**
 * Encrypts and decrypts single personal-data fields. Implementations must be reversible, keyed, and
 * produce ciphertext that reveals no plaintext.
 */
public interface FieldCipher {

	String encrypt(String plaintext);

	String decrypt(String ciphertext);
}
