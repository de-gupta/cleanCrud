package de.gupta.clean.crud.template.useCases.operation.domain.model;

import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.util.Objects;

public final class QuarantineReplayEnvelope
{
	private final String typeKey;
	private final String serialized;

	public static QuarantineReplayEnvelope of(final String typeKey, final String serialized)
	{
		return new QuarantineReplayEnvelope(typeKey, serialized);
	}

	public String typeKey()
	{
		return typeKey;
	}

	public String serialized()
	{
		return serialized;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(typeKey, serialized);
	}

	@Override
	public boolean equals(final Object obj)
	{
		return this == obj || (obj instanceof QuarantineReplayEnvelope other && typeKey.equals(
				other.typeKey) && serialized.equals(other.serialized));
	}

	@Override
	public String toString()
	{
		return "QuarantineReplayEnvelope{typeKey='" + typeKey + "'}";
	}

	private QuarantineReplayEnvelope(final String typeKey, final String serialized)
	{
		StringSanitizationUtility.requireNotBlank(typeKey,
				() -> new IllegalArgumentException("typeKey must not be blank"));
		StringSanitizationUtility.requireNotBlank(serialized,
				() -> new IllegalArgumentException("serialized must not be blank"));
		this.typeKey = typeKey;
		this.serialized = serialized;
	}
}