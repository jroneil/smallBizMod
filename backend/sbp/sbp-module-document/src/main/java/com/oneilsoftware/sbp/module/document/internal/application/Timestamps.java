package com.oneilsoftware.sbp.module.document.internal.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * PostgreSQL {@code timestamptz} stores microsecond precision, so timestamps are truncated to
 * microseconds at creation. This keeps the in-memory value identical to the persisted value, making
 * record equality stable across a save/reload round trip. Internal helper shared across the
 * module's {@code internal} packages; not part of the public contract.
 */
public final class Timestamps {

	private Timestamps() {
	}

	public static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}
}
