package de.gupta.clean.crud.template.useCases.operation.create.application.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.APIToDomainCreateAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.application.adapter.CreationOperationResultAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.application.service.CreateApplicationService;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;

public abstract class AbstractCreateApplicationServiceFacade<ApiPayload extends CreateOperationPayload, DomainPayload extends CreateOperationPayload, DomainModel, APIModel>
		implements CreateApplicationServiceFacade<ApiPayload, APIModel>
{
	private final CreateApplicationService<DomainPayload, DomainModel> service;
	private final APIToDomainCreateAdapter<ApiPayload, DomainPayload> requestAdapter;
	private final CreationOperationResultAdapter<DomainModel, APIModel> resultAdapter;

	@Override
	public CreateApplicationResult<APIModel> create(final CreationOperationRequest<ApiPayload> request)
	{
		return resultAdapter.mapToAPIResult(
				service.create(request.withPayload(requestAdapter.mapToDomainModelCreate(request.payload()))));
	}

	protected AbstractCreateApplicationServiceFacade(
			final CreateApplicationService<DomainPayload, DomainModel> service,
			final APIToDomainCreateAdapter<ApiPayload, DomainPayload> requestAdapter,
			final CreationOperationResultAdapter<DomainModel, APIModel> resultAdapter)
	{
		this.service = service;
		this.requestAdapter = requestAdapter;
		this.resultAdapter = resultAdapter;
	}
}