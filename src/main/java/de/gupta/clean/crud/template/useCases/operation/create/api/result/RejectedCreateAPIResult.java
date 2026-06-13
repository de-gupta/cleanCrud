package de.gupta.clean.crud.template.useCases.operation.create.api.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

public record RejectedCreateAPIResult()
		implements CreateAPIResult
{
	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.REJECTED;
	}
}
