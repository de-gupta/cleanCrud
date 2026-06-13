package de.gupta.clean.crud.template.useCases.operation.create.application.port.in;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;

public interface CreateApplicationController<Payload extends CreateOperationPayload, APIModel>
{
	CreateAPIResult<APIModel> create(final CreationOperationRequest<Payload> request);
}