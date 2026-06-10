package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id;

import java.util.Objects;
import java.util.UUID;

public record CreationQuarantineId(String value)
{
	public static CreationQuarantineId random()
	{
		return new CreationQuarantineId(UUID.randomUUID().toString());
	}

	public CreationQuarantineId
	{
		Objects.requireNonNull(value, "value");
	}
}
