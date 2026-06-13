package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationStatus;

public sealed interface CreateApplicationResult<Model>
		permits CreatedCreateApplicationResult, QuarantinedCreateApplicationResult, RejectedCreateApplicationResult
{
	CreateOperationStatus status();
}