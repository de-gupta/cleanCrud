package de.gupta.clean.crud.template.infrastructure.persistence.history.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class TemporalValidity
{
	private static final Instant DEFAULT_END_VALIDITY =
			ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC).toInstant();

	public static Instant defaultEndValidity()
	{
		return DEFAULT_END_VALIDITY;
	}

	private TemporalValidity()
	{
	}
}