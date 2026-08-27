package com.oneilsoftware.sbp.module.usermaintenance.internal.encryption;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Keyed blind-index/search-token generator. Produces a non-reversible HMAC-SHA-256 token from a
 * normalized value; the token is stored alongside the ciphertext so encrypted personal fields
 * remain searchable by normalized exact match without exposing the plaintext.
 */
public final class SearchTokenGenerator {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private final Mac mac;

	public SearchTokenGenerator(String base64Key) {
		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(base64Key);
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("sbp.usermaintenance.search-key must be Base64-encoded", exception);
		}
		if (keyBytes.length < 32) {
			throw new IllegalArgumentException("sbp.usermaintenance.search-key must decode to at least 256 bits");
		}
		try {
			this.mac = Mac.getInstance(HMAC_ALGORITHM);
			this.mac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to initialize search-token generator", exception);
		}
	}

	public String tokenFor(String value) {
		byte[] digest = mac.doFinal(normalize(value).getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder(digest.length * 2);
		for (byte b : digest) {
			hex.append(HEX[(b >> 4) & 0x0f]).append(HEX[b & 0x0f]);
		}
		return hex.toString();
	}

	/**
	 * Normalization: trim, Unicode NFKC, then lowercase. Applied identically to indexed values and
	 * to the query, so search is case- and diacritic-insensitive exact match.
	 */
	public static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
	}
}
