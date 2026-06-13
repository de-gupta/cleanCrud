package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

public sealed interface CreateOperationResult<Model>
		permits CreatedCreateOperationResult, QuarantinedCreateOperationResult, RejectedCreateOperationResult
{
	CreateOperationStatus status();
}