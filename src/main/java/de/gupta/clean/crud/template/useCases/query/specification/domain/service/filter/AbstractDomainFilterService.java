package de.gupta.clean.crud.template.useCases.query.specification.domain.service.filter;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.DomainFilterPipeline;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchService;

import java.util.Collection;

public abstract class AbstractDomainFilterService<DomainID, DomainModel, DomainModelResponse>
		implements DomainFilterService<DomainID, DomainModel, DomainModelResponse>
{
	private final FetchService<DomainModel, DomainID> fetchService;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> domainResponseBuilder;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> queryBy(
			final DomainFilterPipeline<DomainModel> filterPipeline)
	{
		return fetchService.findAll()
						   .stream()
						   .filter(m -> filterPipeline.allows(m.model()))
						   .map(domainResponseBuilder::toResponse)
						   .toList();
	}

	protected AbstractDomainFilterService(final FetchService<DomainModel, DomainID> fetchService,
										  final DomainResponseBuilder<DomainModel, DomainModelResponse> domainResponseBuilder)
	{
		this.fetchService = fetchService;
		this.domainResponseBuilder = domainResponseBuilder;
	}
}