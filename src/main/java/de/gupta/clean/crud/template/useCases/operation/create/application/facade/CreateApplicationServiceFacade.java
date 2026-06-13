package de.gupta.clean.crud.template.useCases.operation.create.application.facade;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;

public interface CreateApplicationServiceFacade<Payload extends CreateOperationPayload, APIModel>
{
	CreateApplicationResult<APIModel> create(final CreationOperationRequest<Payload> request);
}