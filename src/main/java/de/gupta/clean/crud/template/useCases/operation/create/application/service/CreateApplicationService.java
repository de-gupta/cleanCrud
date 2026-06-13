package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResult;

public interface CreateApplicationService<Payload extends CreateOperationPayload>
{
	CreationOperationResult create(
			final CreationOperationRequest<Payload> request);
}