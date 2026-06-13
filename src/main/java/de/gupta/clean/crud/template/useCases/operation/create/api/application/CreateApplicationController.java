package de.gupta.clean.crud.template.useCases.operation.create.api.application;

import de.gupta.clean.crud.template.useCases.operation.create.api.result.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;

public interface CreateApplicationController<Payload extends CreateOperationPayload, APIModel>
{
	CreateAPIResult<APIModel> create(final CreationOperationRequest<Payload> request);
}
