package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

public record RejectedCreationOperationResult() implements CreationOperationResult
{
	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.REJECTED;
	}
}