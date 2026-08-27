package com.oneilsoftware.sbp.module.job.internal.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * PostgreSQL {@code timestamptz} stores microsecond precision, so timestamps are truncated to
 * microseconds at creation. This keeps the in-memory value identical to the persisted value, making
 * record equality stable across a save/reload round trip.
 */
final class Timestamps {

	private Timestamps() {
	}

	static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}
}
