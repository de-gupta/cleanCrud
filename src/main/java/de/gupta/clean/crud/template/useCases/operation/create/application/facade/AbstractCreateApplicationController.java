package de.gupta.clean.crud.template.useCases.operation.create.application.facade;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.application.port.in.CreateApplicationController;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;

public abstract class AbstractCreateApplicationController<APICreatePayload extends CreateOperationPayload, APIModel>
		implements CreateApplicationController<APICreatePayload, APIModel>
{
	private final CreateApplicationServiceFacade<APICreatePayload, APIModel> serviceFacade;

	@Override
	public CreateApplicationResult<APIModel> create(final CreateOperationRequest<APICreatePayload> request)
	{
		return serviceFacade.create(request);
	}

	protected AbstractCreateApplicationController(
			final CreateApplicationServiceFacade<APICreatePayload, APIModel> serviceFacade)
	{
		this.serviceFacade = serviceFacade;
	}
}