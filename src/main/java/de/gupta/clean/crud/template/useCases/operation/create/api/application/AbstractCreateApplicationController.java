package de.gupta.clean.crud.template.useCases.operation.create.api.application;

import de.gupta.clean.crud.template.useCases.operation.create.api.result.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.facade.CreateApplicationServiceFacade;

public abstract class AbstractCreateApplicationController<Payload extends CreateOperationPayload, APIModel>
		implements CreateApplicationController<Payload, APIModel>
{
	private final CreateApplicationServiceFacade<Payload, APIModel> serviceFacade;

	@Override
	public CreateAPIResult<APIModel> create(final CreationOperationRequest<Payload> request)
	{
		return serviceFacade.create(request);
	}

	protected AbstractCreateApplicationController(final CreateApplicationServiceFacade<Payload, APIModel> serviceFacade)
	{
		this.serviceFacade = serviceFacade;
	}
}
