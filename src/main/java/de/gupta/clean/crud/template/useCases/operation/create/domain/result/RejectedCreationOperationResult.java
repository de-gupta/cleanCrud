package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

public record RejectedCreationOperationResult<Model>() implements CreationOperationResult<Model>
{
	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.REJECTED;
	}
}