package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import java.util.Objects;

public record SerializedMutationValue(
		String valueType,
		String valueJson)
{
	public SerializedMutationValue
	{
		Objects.requireNonNull(valueType, "valueType");
		Objects.requireNonNull(valueJson, "valueJson");
	}
}
