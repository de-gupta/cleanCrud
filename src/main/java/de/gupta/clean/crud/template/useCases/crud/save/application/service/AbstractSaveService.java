package de.gupta.clean.crud.template.useCases.crud.save.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;

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
	private final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModelResponse> save(final MasterDomainModelCreate model)
	{
		return saveAll(List.of(model)).stream().findFirst().orElseThrow();
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModelResponse>> saveAll(
			final Collection<MasterDomainModelCreate> models)
	{
		return engine.saveAll(definition, models).stream().map(this::identifiedModel).toList();
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModelResponse> identifiedModel(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> identifiedModel)
	{
		return IdentifiedModel.of(identifiedModel.id(),
				definition.responseBuilder().toResponse(identifiedModel.model()));
	}

	protected AbstractSaveService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		this.definition = definition;
		this.engine = engine;
	}
}