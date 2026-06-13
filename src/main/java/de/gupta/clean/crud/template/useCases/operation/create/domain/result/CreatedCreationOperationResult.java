package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import java.util.Objects;

public record CreatedCreationOperationResult<Model>(Model createdModel)
		implements CreationOperationResult
{
	public CreatedCreationOperationResult
	{
		Objects.requireNonNull(createdModel, "createdModel");
	}

	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.CREATED;
	}
}
