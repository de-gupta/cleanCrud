package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractFetchService<DomainID, DomainModel>
		implements FetchService<DomainModel, DomainID>
{
	private final FetchPersistenceService<DomainID, DomainModel> persistenceService;
	private final DomainFilterPipeline<DomainModel> baseFilterPipeline;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModel>> findAll()
	{
		return persistenceService.findAll()
								 .stream()
								 .filter(this::isVisible)
								 .toList();
	}

	@Override
	public Page<IdentifiedModel<DomainID, DomainModel>> findAll(final Pageable pageable)
	{
		return PageUtility.filterPage(persistenceService.findAll(pageable), this::isVisible);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModel> findById(final DomainID domainID)
	{
		return persistenceService.findById(domainID)
								 .filter(this::isVisible)
								 .orElseThrow(() -> ResourceNotFoundException.withId(domainID));
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModel>> findByIds(final Set<DomainID> IDs)
	{
		return persistenceService.findByIds(IDs).stream()
								 .filter(this::isVisible)
								 .toList();
	}

	private boolean isVisible(IdentifiedModel<DomainID, DomainModel> identifiedModel)
	{
		return baseFilterPipeline.allows(identifiedModel.model());
	}

	protected AbstractFetchService(
			final FetchPersistenceService<DomainID, DomainModel> persistenceService,
			final DomainSecurityPolicy<DomainModel> domainSecurityPolicy)
	{
		this.persistenceService = persistenceService;
		this.baseFilterPipeline = DomainFilterPipeline.of(domainSecurityPolicy::isAccessAllowed);
	}
}