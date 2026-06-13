package de.gupta.clean.crud.template.useCases.operation.create.application.model;

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