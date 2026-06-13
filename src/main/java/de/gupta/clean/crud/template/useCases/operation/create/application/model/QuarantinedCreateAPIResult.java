package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

public record QuarantinedCreateAPIResult<Model>()
		implements CreateAPIResult<Model>
{
	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.QUARANTINED;
	}
}