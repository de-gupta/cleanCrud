package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

public sealed interface CreateAPIResult<Model>
		permits CreatedCreateAPIResult, QuarantinedCreateAPIResult, RejectedCreateAPIResult
{
	CreationOperationStatus status();
}