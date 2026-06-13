package de.gupta.clean.crud.template.useCases.operation.create.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.APIToDomainCreateAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.api.application.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.api.application.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.application.service.CreateApplicationService;
import de.gupta.clean.crud.template.useCases.operation.domain.metadata.request.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.domain.metadata.result.CreationOperationResult;
import de.gupta.clean.crud.template.useCases.operation.domain.metadata.result.CreationOperationResultFactory;

public abstract class AbstractCreateApplicationServiceFacade<Payload extends CreateOperationPayload, APIModelID, APIModelResponse,
		CreateAPIResult extends CreationOperationResult<APIModelID, APIModelResponse>, DomainID, DomainModelCreate, DomainModelResponse>
		implements CreateApplicationServiceFacade<Payload, APIModelID, APIModelResponse, CreateAPIResult>
{
	private final CreateApplicationService<DomainID, DomainModelCreate, DomainModelResponse> service;
	private final APIToDomainCreateAdapter<Payload, DomainModelCreate> requestAdapter;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseAdapter;

	@Override
	public CreateAPIResult create(final CreationOperationRequest<Payload> request,
	                              final OperationRequestMetadata requestMetadata)
	{
		return CreationOperationResultFactory.create(
				responseAdapter.mapToAPIModelResponse(
						service.create(
								requestAdapter.mapToDomainModelCreate(request.payload()), requestMetadata).model()));
	}

	protected AbstractCreateApplicationServiceFacade(
			final CreateApplicationService<DomainID, DomainModelCreate, DomainModelResponse> service,
			final APIToDomainCreateAdapter<Payload, DomainModelCreate> requestAdapter,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseAdapter)
	{
		this.service = service;
		this.requestAdapter = requestAdapter;
		this.responseAdapter = responseAdapter;
	}
}