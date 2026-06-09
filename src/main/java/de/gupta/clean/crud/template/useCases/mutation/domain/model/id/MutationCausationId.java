package de.gupta.clean.crud.template.useCases.mutation.domain.model.id;

import java.util.Objects;

public record MutationCausationId(String value)
{
	public static MutationCausationId of(final String value)
	{
		return new MutationCausationId(Objects.requireNonNull(value, "value"));
	}

	public MutationCausationId
	{
		if (value == null || value.isBlank())
		{
			throw new IllegalArgumentException("value");
		}
	}
}