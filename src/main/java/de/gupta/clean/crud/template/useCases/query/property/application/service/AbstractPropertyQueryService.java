package de.gupta.clean.crud.template.useCases.query.property.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.query.PropertyExtractor;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.DomainFilterPipeline;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchService;
import de.gupta.commons.utility.comparison.ComparisonType;
import de.gupta.commons.utility.comparison.ComparisonUtility;

import java.util.Collection;
import java.util.Comparator;

public abstract class AbstractPropertyQueryService<Property, DomainID, DomainModel, DomainModelResponse>
		implements PropertyQueryService<Property, DomainID, DomainModelResponse>
{
	private final PropertyExtractor<DomainModel, Property> propertyExtractor;
	private final Comparator<Property> propertyComparator;
	private final FetchService<DomainModel, DomainID> fetchService;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> domainResponseBuilder;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> queryBy(final String propertyName,
																			  final Property property,
																			  final ComparisonType comparisonType)
	{
		final DomainFilterPipeline<DomainModel> dynamicFilter = DomainFilterPipeline.of(model ->
				ComparisonUtility.doesThisValueSatisfyTheComparison(
						propertyExtractor.extractProperty(propertyName, model),
						property,
						comparisonType,
						propertyComparator)
		);

		return fetchService.findAll()
						   .stream()
						   .filter(m -> dynamicFilter.allows(m.model()))
						   .map(domainResponseBuilder::toResponse)
						   .toList();
	}

	protected AbstractPropertyQueryService(final FetchService<DomainModel, DomainID> fetchService,
										   final PropertyExtractor<DomainModel, Property> propertyExtractor,
										   final Comparator<Property> propertyComparator,
										   final DomainResponseBuilder<DomainModel, DomainModelResponse> domainResponseBuilder)
	{
		this.fetchService = fetchService;
		this.propertyExtractor = propertyExtractor;
		this.propertyComparator = propertyComparator;
		this.domainResponseBuilder = domainResponseBuilder;
	}
}