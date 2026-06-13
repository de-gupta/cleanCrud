package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

public record QuarantinedCreationOperationResult<Model>() implements CreationOperationResult<Model>
{
	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.QUARANTINED;
	}
}