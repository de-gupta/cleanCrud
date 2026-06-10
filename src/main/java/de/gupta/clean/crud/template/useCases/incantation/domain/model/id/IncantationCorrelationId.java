package de.gupta.clean.crud.template.useCases.incantation.domain.model.id;

import java.util.Objects;

public record IncantationCorrelationId(String value)
{
	public IncantationCorrelationId
	{
		Objects.requireNonNull(value, "value");
		if (value.isBlank())
		{
			throw new IllegalArgumentException("value");
		}
	}
}
