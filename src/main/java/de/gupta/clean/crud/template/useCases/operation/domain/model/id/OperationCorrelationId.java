package de.gupta.clean.crud.template.useCases.operation.domain.model.id;

import java.util.Objects;

public record OperationCorrelationId(String value)
{
	public OperationCorrelationId
	{
		Objects.requireNonNull(value, "value");
	}
}