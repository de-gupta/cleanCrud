package de.gupta.clean.crud.template.useCases.process.domain.model.id;

import java.util.UUID;

public record DurableProcessTaskId(String value)
{
	public static DurableProcessTaskId random()
	{
		return new DurableProcessTaskId(UUID.randomUUID().toString());
	}

	public DurableProcessTaskId
	{
		if (value == null || value.isBlank())
		{
			throw new IllegalArgumentException("value");
		}
	}
}