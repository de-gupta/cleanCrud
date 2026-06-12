package de.gupta.clean.crud.template.useCases.operationOLD.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public final class QuarantineReplayEnvelope
{
	private final String typeKey;
	private final String serialized;

	@JsonCreator
	public static QuarantineReplayEnvelope of(
			@JsonProperty("typeKey") final String typeKey,
			@JsonProperty("serialized") final String serialized)
	{
		return new QuarantineReplayEnvelope(typeKey, serialized);
	}

	@JsonProperty("typeKey")
	public String typeKey()
	{
		return typeKey;
	}

	@JsonProperty("serialized")
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