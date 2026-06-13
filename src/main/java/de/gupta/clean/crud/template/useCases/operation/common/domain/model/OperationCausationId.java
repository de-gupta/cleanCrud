package de.gupta.clean.crud.template.useCases.operation.common.domain.model;

import java.util.Objects;

public record OperationCausationId(String value)
{
	public OperationCausationId
	{
		Objects.requireNonNull(value, "value");
	}
}
