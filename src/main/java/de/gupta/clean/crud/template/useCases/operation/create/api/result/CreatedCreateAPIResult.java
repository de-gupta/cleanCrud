package de.gupta.clean.crud.template.useCases.operation.create.api.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

import java.util.Objects;

public record CreatedCreateAPIResult<Model>(Model createdModel)
		implements CreateAPIResult<Model>
{
	public CreatedCreateAPIResult
	{
		Objects.requireNonNull(createdModel, "createdModel");
	}

	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.CREATED;
	}
}
