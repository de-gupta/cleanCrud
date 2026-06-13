package de.gupta.clean.crud.template.useCases.operation.create.facade;

import de.gupta.clean.crud.template.useCases.operation.create.api.application.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.api.application.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.domain.metadata.request.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.domain.metadata.result.CreationOperationResult;

public interface CreateApplicationServiceFacade<Payload extends CreateOperationPayload, APIModelID, APIModelResponse,
		CreateAPIResult extends CreationOperationResult<APIModelID, APIModelResponse>>
{
	CreateAPIResult create(final CreationOperationRequest<Payload> request,
	                       final OperationRequestMetadata requestMetadata);
}