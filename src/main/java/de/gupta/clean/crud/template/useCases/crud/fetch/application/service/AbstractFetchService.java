package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.aggregate.operation.AggregateFetchService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractFetchService<MasterDomainId, MasterDomainModel>
		implements FetchService<MasterDomainModel, MasterDomainId>
{
	private final AggregateFetchService<MasterDomainId, MasterDomainModel> aggregateFetchService;

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll()
	{
		return aggregateFetchService.findAll();
	}

	@Override
	public Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(final Pageable pageable)
	{
		return aggregateFetchService.findAll(pageable);
	}

	@Override
	public IdentifiedModel<MasterDomainId, MasterDomainModel> findById(final MasterDomainId domainID)
	{
		return aggregateFetchService.findById(domainID);
	}

	@Override
	public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findByIds(final Set<MasterDomainId> ids)
	{
		return aggregateFetchService.findByIds(ids);
	}

	protected AbstractFetchService(
			final AggregateFetchService<MasterDomainId, MasterDomainModel> aggregateFetchService)
	{
		this.aggregateFetchService = aggregateFetchService;
	}
}