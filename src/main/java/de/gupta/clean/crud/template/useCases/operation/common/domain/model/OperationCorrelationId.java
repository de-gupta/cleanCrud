package de.gupta.clean.crud.template.useCases.operation.common.domain.model;

import java.util.Objects;

public record OperationCorrelationId(String value)
{
	public OperationCorrelationId
	{
		Objects.requireNonNull(value, "value");
	}
}
