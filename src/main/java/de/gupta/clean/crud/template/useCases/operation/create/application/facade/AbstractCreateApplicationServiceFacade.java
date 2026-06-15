package de.gupta.clean.crud.template.useCases.operation.create.application.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.APIToDomainCreateAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.application.adapter.CreateOperationResultAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.application.service.CreateApplicationService;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;

public abstract class AbstractCreateApplicationServiceFacade<APIPayload extends CreateOperationPayload, DomainPayload extends CreateOperationPayload, DomainModel, APIModel>
		implements CreateApplicationServiceFacade<APIPayload, APIModel>
{
	private final CreateApplicationService<DomainPayload, DomainModel> service;
	private final APIToDomainCreateAdapter<APIPayload, DomainPayload> requestAdapter;
	private final CreateOperationResultAdapter<DomainModel, APIModel> resultAdapter;

	@Override
	public CreateApplicationResult<APIModel> create(final CreateOperationRequest<APIPayload> request)
	{
		return resultAdapter.mapToAPIResult(
				service.create(request.withPayload(requestAdapter.mapToDomainModelCreate(request.payload()))));
	}

	protected AbstractCreateApplicationServiceFacade(
			final CreateApplicationService<DomainPayload, DomainModel> service,
			final APIToDomainCreateAdapter<APIPayload, DomainPayload> requestAdapter,
			final CreateOperationResultAdapter<DomainModel, APIModel> resultAdapter)
	{
		this.service = service;
		this.requestAdapter = requestAdapter;
		this.resultAdapter = resultAdapter;
	}
}