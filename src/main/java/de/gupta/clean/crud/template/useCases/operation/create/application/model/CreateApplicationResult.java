package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

public sealed interface CreateApplicationResult<Model>
		permits CreatedCreateApplicationResult, QuarantinedCreateApplicationResult, RejectedCreateApplicationResult
{
	CreationOperationStatus status();
}