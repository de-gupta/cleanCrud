package de.gupta.clean.crud.template.useCases.operation.domain.model.id;

import java.util.Objects;

public record OperationCausationId(String value)
{
	public OperationCausationId
	{
		Objects.requireNonNull(value, "value");
	}
}