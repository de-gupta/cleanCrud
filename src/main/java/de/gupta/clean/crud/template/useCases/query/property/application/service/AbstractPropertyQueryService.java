package de.gupta.clean.crud.template.useCases.query.property.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.AbstractFetchService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.DomainFilterPipeline;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;
import de.gupta.commons.utility.comparison.ComparisonType;
import de.gupta.commons.utility.comparison.ComparisonUtility;

import java.util.Collection;
import java.util.Comparator;

public abstract class AbstractPropertyQueryService<Property, DomainID, DomainModel, DomainModelResponse>
		extends AbstractFetchService<DomainModelResponse, DomainID, DomainModel>
		implements PropertyQueryService<Property, DomainID, DomainModelResponse>
{
	private final PropertyExtractor<DomainModel, Property> propertyExtractor;
	private final Comparator<Property> propertyComparator;

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

		return filterAndMap(findAllRaw(), dynamicFilter);
	}

	protected AbstractPropertyQueryService(final FetchPersistenceService<DomainID, DomainModel> persistenceService,
										   final DomainResponseBuilder<DomainModel, DomainModelResponse> modelMapper,
										   final DomainSecurityPolicy<DomainModel> domainSecurityPolicy,
										   final PropertyExtractor<DomainModel, Property> propertyExtractor,
										   final Comparator<Property> propertyComparator
	)
	{
		super(persistenceService, modelMapper, domainSecurityPolicy);
		this.propertyExtractor = propertyExtractor;
		this.propertyComparator = propertyComparator;
	}
}