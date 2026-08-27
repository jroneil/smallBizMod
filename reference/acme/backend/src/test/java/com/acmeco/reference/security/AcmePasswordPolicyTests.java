package com.acmeco.reference.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Verifies the single authoritative password rule applied identically to manual and generated
 * resets. Both paths delegate to the same server-side policy rather than to per-form length checks.
 */
class AcmePasswordPolicyTests {

	private final AcmePasswordPolicy policy = new AcmePasswordPolicy();

	@Test
	void acceptsMinimumAndMaximumLengths() {
		assertThatCode(() -> policy.validate("a".repeat(8))).doesNotThrowAnyException();
		assertThatCode(() -> policy.validate("a".repeat(72))).doesNotThrowAnyException();
	}

	@Test
	void acceptsAGeneratedStylePassword() {
		assertThatCode(() -> policy.validate("Vx9!kQ2#mT$7zR4w")).doesNotThrowAnyException();
	}

	@Test
	void rejectsTooShortAndTooLongPasswords() {
		assertThatThrownBy(() -> policy.validate("a".repeat(7)))
				.isInstanceOf(InvalidPasswordException.class);
		assertThatThrownBy(() -> policy.validate("a".repeat(73)))
				.isInstanceOf(InvalidPasswordException.class);
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> policy.validate(null))
				.isInstanceOf(InvalidPasswordException.class);
	}
}
