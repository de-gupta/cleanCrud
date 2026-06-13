package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

public sealed interface CreationOperationResult<Model>
		permits CreatedCreationOperationResult, QuarantinedCreationOperationResult, RejectedCreationOperationResult
{
	CreationOperationStatus status();
}
