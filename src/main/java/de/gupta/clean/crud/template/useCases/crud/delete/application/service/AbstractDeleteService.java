package de.gupta.clean.crud.template.useCases.crud.delete.application.service;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;

import java.util.Collection;

public abstract class AbstractDeleteService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements DeleteService<MasterDomainId>
{
	private final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;

	@Override
	public void deleteById(final MasterDomainId id)
	{
		engine.deleteById(definition, id);
	}

	@Override
	public void deleteAllById(final Collection<MasterDomainId> ids, final BulkOperationMode mode)
	{
		engine.deleteAllById(definition, ids, mode);
	}

	protected AbstractDeleteService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		this.definition = definition;
		this.engine = engine;
	}
}