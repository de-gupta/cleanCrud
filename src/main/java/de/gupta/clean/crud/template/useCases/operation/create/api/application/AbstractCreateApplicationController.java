package de.gupta.clean.crud.template.useCases.operation.create.api.application;

import de.gupta.clean.crud.template.useCases.operation.create.facade.CreateApplicationServiceFacade;
import de.gupta.clean.crud.template.useCases.operation.domain.metadata.request.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.domain.metadata.result.CreationOperationResult;

public abstract class AbstractCreateApplicationController<Payload extends CreateOperationPayload, APIModelID, APIModelResponse, CreateAPIResult extends CreationOperationResult<APIModelID, APIModelResponse>>
		implements CreateApplicationController<Payload, APIModelID, APIModelResponse, CreateAPIResult>
{
	private final CreateApplicationServiceFacade<Payload, CreateAPIResult> serviceFacade;

	@Override
	public CreateAPIResult create(final CreationOperationRequest<Payload> request,
	                              final OperationRequestMetadata requestMetadata)
	{
		return serviceFacade.create(request, requestMetadata);
	}

	protected AbstractCreateApplicationController(
			final CreateApplicationServiceFacade<Payload, CreateAPIResult> serviceFacade)
	{
		this.serviceFacade = serviceFacade;
	}
}