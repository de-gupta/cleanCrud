package de.gupta.clean.crud.template.useCases.mutation.domain.model.id;

import java.util.Objects;

public record MutationCorrelationId(String value)
{
	public static MutationCorrelationId of(final String value)
	{
		return new MutationCorrelationId(Objects.requireNonNull(value, "value"));
	}

	public MutationCorrelationId
	{
		if (value == null || value.isBlank())
		{
			throw new IllegalArgumentException("value");
		}
	}
}