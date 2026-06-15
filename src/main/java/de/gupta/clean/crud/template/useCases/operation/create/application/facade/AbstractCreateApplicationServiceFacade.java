package de.gupta.clean.crud.template.useCases.operation.create.application.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.APIToDomainCreateAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.application.adapter.CreateOperationResultAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateApplicationResult;
import de.gupta.clean.crud.template.useCases.operation.create.application.service.CreateApplicationService;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;

public abstract class AbstractCreateApplicationServiceFacade<APICreatePayload extends CreateOperationPayload, DomainCreatePayload extends CreateOperationPayload, DomainModel, APIModel>
		implements CreateApplicationServiceFacade<APICreatePayload, APIModel>
{
	private final APIToDomainCreateAdapter<APICreatePayload, DomainCreatePayload> requestAdapter;
	private final CreateApplicationService<DomainCreatePayload, DomainModel> service;
	private final CreateOperationResultAdapter<DomainModel, APIModel> resultAdapter;

	@Override
	public CreateApplicationResult<APIModel> create(final CreateOperationRequest<APICreatePayload> request)
	{
		return resultAdapter.mapToAPIResult(
				service.create(request.withPayload(requestAdapter.mapToDomainModelCreate(request.payload()))));
	}

	protected AbstractCreateApplicationServiceFacade(
			final APIToDomainCreateAdapter<APICreatePayload, DomainCreatePayload> requestAdapter,
			final CreateApplicationService<DomainCreatePayload, DomainModel> service,
			final CreateOperationResultAdapter<DomainModel, APIModel> resultAdapter)
	{
		this.requestAdapter = requestAdapter;
		this.service = service;
		this.resultAdapter = resultAdapter;
	}
}