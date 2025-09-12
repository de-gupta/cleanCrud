package de.gupta.clean.crud.template.useCases.crud.fetch.application.service.filter;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.DomainFilterPipeline;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;

import java.util.Collection;

public abstract class AbstractDomainFilterService<DomainID, DomainModel, DomainModelResponse>
		implements DomainFilterService<DomainID, DomainModel, DomainModelResponse>
{
	private final FetchPersistenceService<DomainID, DomainModel> fetchPersistenceService;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> domainResponseBuilder;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> filter(
			final DomainFilterPipeline<DomainModel> filterPipeline)
	{
		return fetchPersistenceService.findAll()
									  .stream()
									  .filter(m -> filterPipeline.allows(m.model()))
									  .map(domainResponseBuilder::toResponse)
									  .toList();
	}

	protected AbstractDomainFilterService(final FetchPersistenceService<DomainID, DomainModel> fetchPersistenceService,
										  final DomainResponseBuilder<DomainModel, DomainModelResponse> domainResponseBuilder)
	{
		this.fetchPersistenceService = fetchPersistenceService;
		this.domainResponseBuilder = domainResponseBuilder;
	}
}