package de.gupta.clean.crud.template.useCases.operation.create.application.port.in;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;

public interface CreateApplicationController<Payload extends CreateOperationPayload, APIModel>
{
	CreateApplicationResult<APIModel> create(final CreateOperationRequest<Payload> request);
}