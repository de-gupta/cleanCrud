package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model;

import java.util.Objects;
import java.util.UUID;

public record QuarantineId(String value)
{
	public static QuarantineId random()
	{
		return new QuarantineId(UUID.randomUUID().toString());
	}

	public QuarantineId
	{
		Objects.requireNonNull(value, "value");
	}
}