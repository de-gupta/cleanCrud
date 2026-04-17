package de.gupta.clean.crud.template.useCases.crud.aggregate.port;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public final class AggregateFetchPortAdapter<DomainId, DomainModel> implements AggregateFetchPort<DomainId, DomainModel>
{
	private final FetchPersistenceService<DomainId, DomainModel> fetchPersistenceService;

	public AggregateFetchPortAdapter(final FetchPersistenceService<DomainId, DomainModel> fetchPersistenceService)
	{
		this.fetchPersistenceService = fetchPersistenceService;
	}

	@Override
	public Optional<IdentifiedModel<DomainId, DomainModel>> findById(final DomainId domainId)
	{
		return fetchPersistenceService.findById(domainId);
	}

	@Override
	public Collection<IdentifiedModel<DomainId, DomainModel>> findByIds(final Set<DomainId> domainIds)
	{
		return fetchPersistenceService.findByIds(domainIds);
	}

	@Override
	public Collection<IdentifiedModel<DomainId, DomainModel>> findAll()
	{
		return fetchPersistenceService.findAll();
	}

	@Override
	public Slice<IdentifiedModel<DomainId, DomainModel>> findAll(final Pageable pageable)
	{
		return fetchPersistenceService.findAll(pageable);
	}
}
