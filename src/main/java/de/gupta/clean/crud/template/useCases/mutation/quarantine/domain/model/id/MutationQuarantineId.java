package de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id;

import java.util.Objects;
import java.util.UUID;

public record MutationQuarantineId(String value)
{
	public static MutationQuarantineId random()
	{
		return new MutationQuarantineId(UUID.randomUUID().toString());
	}

	public MutationQuarantineId
	{
		Objects.requireNonNull(value, "value");
	}
}
