package de.gupta.clean.crud.template.useCases.query.specification.collection.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.query.specification.collection.application.service.adapter.FilterSpecificationToDomainFilterAdapter;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;
import de.gupta.clean.crud.template.useCases.query.specification.domain.service.filter.DomainFilterService;

import java.util.Collection;

public abstract class AbstractSpecificationQueryService<DomainID, DomainModel, DomainModelResponse>
		implements SpecificationQueryService<DomainID, DomainModelResponse>
{
	private final DomainFilterService<DomainID, DomainModel, DomainModelResponse> filterService;
	private final FilterSpecificationToDomainFilterAdapter<DomainModel> filterAdapter;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> queryBy(
			final FilterSpecification filterSpecification)
	{
		return filterService.queryBy(filterAdapter.domainFilterPipeline(filterSpecification));
	}

	protected AbstractSpecificationQueryService(
			final DomainFilterService<DomainID, DomainModel, DomainModelResponse> filterService,
			final FilterSpecificationToDomainFilterAdapter<DomainModel> filterAdapter)
	{
		this.filterService = filterService;
		this.filterAdapter = filterAdapter;
	}
}