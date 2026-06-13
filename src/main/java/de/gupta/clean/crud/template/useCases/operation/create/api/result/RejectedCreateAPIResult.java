package de.gupta.clean.crud.template.useCases.operation.create.api.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

public record RejectedCreateAPIResult<Model>()
		implements CreateAPIResult<Model>
{
	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.REJECTED;
	}
}
