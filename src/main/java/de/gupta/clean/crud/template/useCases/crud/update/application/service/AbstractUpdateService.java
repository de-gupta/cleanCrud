package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;

import java.util.Collection;

public abstract class AbstractUpdateService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements UpdateService<MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse,
		MasterDomainId>
{
	private final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;

	@Override
	public void putAtId(final MasterDomainId id, final MasterDomainModelCreate model)
	{
		engine.putAtId(definition, id, model);
	}

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModelResponse> updateById(
			final MasterDomainId id,
			final MasterDomainModelUpdatePatch updatePatch)
	{
		return identifiedModel(engine.updateById(definition, id, updatePatch));
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModelResponse>> updateAllById(
			final Collection<IdentifiedModel<MasterDomainId, MasterDomainModelUpdatePatch>> models,
			final BulkOperationMode mode)
	{
		return engine.updateAllById(definition, models, mode).stream().map(this::identifiedModel).toList();
	}

	private IdentifiedModel<MasterDomainId, MasterDomainModelResponse> identifiedModel(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), definition.responseBuilder().toResponse(domainModel.model()));
	}

	protected AbstractUpdateService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		this.definition = definition;
		this.engine = engine;
	}
}
