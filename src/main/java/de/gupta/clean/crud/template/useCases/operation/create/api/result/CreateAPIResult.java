package de.gupta.clean.crud.template.useCases.operation.create.api.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

public sealed interface CreateAPIResult<Model>
		permits CreatedCreateAPIResult, QuarantinedCreateAPIResult, RejectedCreateAPIResult
{
	CreationOperationStatus status();
}
