package de.gupta.clean.crud.template.useCases.operation.create.application.port.in;

import de.gupta.clean.crud.template.useCases.operation.create.application.facade.CreateApplicationServiceFacade;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;

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