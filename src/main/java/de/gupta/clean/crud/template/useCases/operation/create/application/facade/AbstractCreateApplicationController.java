package de.gupta.clean.crud.template.useCases.operation.create.application.facade;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.application.port.in.CreateApplicationController;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;

public abstract class AbstractCreateApplicationController<Payload extends CreateOperationPayload, APIModel>
		implements CreateApplicationController<Payload, APIModel>
{
	private final CreateApplicationServiceFacade<Payload, APIModel> serviceFacade;

	@Override
	public CreateApplicationResult<APIModel> create(final CreationOperationRequest<Payload> request)
	{
		return serviceFacade.create(request);
	}

	protected AbstractCreateApplicationController(final CreateApplicationServiceFacade<Payload, APIModel> serviceFacade)
	{
		this.serviceFacade = serviceFacade;
	}
}