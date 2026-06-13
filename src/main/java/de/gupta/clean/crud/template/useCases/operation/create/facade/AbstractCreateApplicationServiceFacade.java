package de.gupta.clean.crud.template.useCases.operation.create.facade;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.APIToDomainCreateAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.operation.create.api.result.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.api.result.CreateAPIResults;
import de.gupta.clean.crud.template.useCases.operation.create.application.service.CreateApplicationService;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreatedCreationOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.QuarantinedCreationOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.RejectedCreationOperationResult;

public abstract class AbstractCreateApplicationServiceFacade<ApiPayload extends CreateOperationPayload, DomainPayload extends CreateOperationPayload, APIModelResponse, DomainID, DomainModelResponse>
		implements CreateApplicationServiceFacade<ApiPayload>
{
	private final CreateApplicationService<DomainPayload> service;
	private final APIToDomainCreateAdapter<ApiPayload, DomainPayload> requestAdapter;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseAdapter;

	@Override
	public CreateAPIResult create(final CreationOperationRequest<ApiPayload> request)
	{
		final var domainRequest = request.withPayload(requestAdapter.mapToDomainModelCreate(request.payload()));
		return mapResult(service.create(domainRequest));
	}

	// TODO: NO NO NO! use type safe adapters and use pattern matching switch - we're in java 25!!! and no ? wildcards
	protected CreateAPIResult mapResult(final CreationOperationResult domainResult)
	{
		if (domainResult instanceof CreatedCreationOperationResult<?>(Object model))
		{
			@SuppressWarnings("unchecked") final var createdModel =
					(IdentifiedModel<DomainID, DomainModelResponse>) model;
			return CreateAPIResults.created(mapToAPIModelResponse(createdModel));
		}
		if (domainResult instanceof QuarantinedCreationOperationResult)
		{
			return CreateAPIResults.quarantined();
		}
		if (domainResult instanceof RejectedCreationOperationResult)
		{
			return CreateAPIResults.rejected();
		}
		throw new IllegalStateException(
				"Unsupported creation operation result type: " + domainResult.getClass().getName());
	}

	protected final APIModelResponse mapToAPIModelResponse(
			final IdentifiedModel<DomainID, DomainModelResponse> createdModel)
	{
		return responseAdapter.mapToAPIModelResponse(createdModel);
	}

	protected AbstractCreateApplicationServiceFacade(
			final CreateApplicationService<DomainPayload> service,
			final APIToDomainCreateAdapter<ApiPayload, DomainPayload> requestAdapter,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseAdapter)
	{
		this.service = service;
		this.requestAdapter = requestAdapter;
		this.responseAdapter = responseAdapter;
	}
}