package de.gupta.clean.crud.template.useCases.process.domain.model.id;

import java.util.UUID;

public record CorrelationId(String value)
{
	public static CorrelationId random()
	{
		return new CorrelationId(UUID.randomUUID().toString());
	}

	public CorrelationId
	{
		if (value == null || value.isBlank())
		{
			throw new IllegalArgumentException("value");
		}
	}
}