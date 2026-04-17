package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractFetchService<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		MasterDomainModelResponse>
		implements FetchService<MasterDomainModel, MasterDomainId>
{
	private final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll()
	{
		return engine.findAll(definition);
	}

	@Override
	public Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(final Pageable pageable)
	{
		return PageUtility.mapSlice(engine.findAll(definition, pageable), identifiedModel -> identifiedModel);
	}

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModel> findById(final MasterDomainId domainID)
	{
		return engine.findById(definition, domainID);
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findByIds(final Set<MasterDomainId> IDs)
	{
		return engine.findByIds(definition, IDs);
	}

	protected AbstractFetchService(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final AggregateLifecycleEngine engine)
	{
		this.definition = definition;
		this.engine = engine;
	}
}