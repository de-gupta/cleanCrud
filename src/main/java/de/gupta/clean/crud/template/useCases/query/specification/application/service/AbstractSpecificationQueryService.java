package de.gupta.clean.crud.template.useCases.query.specification.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchService;
import de.gupta.clean.crud.template.useCases.query.specification.application.service.adapter.FilterSpecificationToDomainFilterAdapter;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Collection;

public abstract class AbstractSpecificationQueryService<DomainID, DomainModel, DomainModelResponse>
		implements SpecificationQueryService<DomainID, DomainModelResponse>
{
	private final FetchService<DomainModel, DomainID> fetchService;
	private final FilterSpecificationToDomainFilterAdapter<DomainModel> filterAdapter;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> domainResponseBuilder;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> queryBy(
			final FilterSpecification filterSpecification)
	{
		return fetchService.findAll()
						   .stream()
						   .filter(m -> filterAdapter.domainFilterPipeline(filterSpecification).allows(m.model()))
						   .map(domainResponseBuilder::toResponse)
						   .toList();
	}

	protected AbstractSpecificationQueryService(final FetchService<DomainModel, DomainID> fetchService,
												final FilterSpecificationToDomainFilterAdapter<DomainModel> filterAdapter,
												final DomainResponseBuilder<DomainModel, DomainModelResponse> domainResponseBuilder)
	{
		this.fetchService = fetchService;
		this.filterAdapter = filterAdapter;
		this.domainResponseBuilder = domainResponseBuilder;
	}
}