package de.gupta.clean.crud.template.useCases.operation.create.facade;

import de.gupta.clean.crud.template.useCases.operation.create.api.result.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;

public interface CreateApplicationServiceFacade<Payload extends CreateOperationPayload>
{
	CreateAPIResult create(final CreationOperationRequest<Payload> request);
}