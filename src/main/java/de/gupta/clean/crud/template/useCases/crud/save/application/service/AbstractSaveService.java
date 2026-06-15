package de.gupta.clean.crud.template.useCases.crud.save.application.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.aggregate.AggregateSaveService;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;

public abstract class AbstractSaveService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements SaveService<MasterDomainModelCreate, MasterDomainModelResponse, MasterDomainId>
{
	private final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateSaveService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate> aggregateSaveService;

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModelResponse> save(final MasterDomainModelCreate model)
	{
		return saveAll(List.of(model)).stream().findFirst().orElseThrow();
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModelResponse>> saveAll(
			final Collection<MasterDomainModelCreate> models)
	{
		return aggregateSaveService.saveAll(models, this::durableProcessStartRequests)
		                           .stream()
		                           .map(this::identifiedModel)
		                           .toList();
	}

	protected Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(
			final Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> savedModels)
	{
		return List.of();
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModelResponse> identifiedModel(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> identifiedModel)
	{
		return IdentifiedModel.of(identifiedModel.id(),
				definition.responseBuilder().toResponse(identifiedModel.model()));
	}

	protected AbstractSaveService(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateSaveService<MasterDomainId, MasterDomainModel, MasterDomainModelCreate> aggregateSaveService)
	{
		this.definition = definition;
		this.aggregateSaveService = aggregateSaveService;
	}
}