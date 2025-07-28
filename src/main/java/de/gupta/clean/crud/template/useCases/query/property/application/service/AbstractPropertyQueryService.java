package de.gupta.clean.crud.template.useCases.query.property.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;
import de.gupta.commons.utility.comparison.ComparisonType;
import de.gupta.commons.utility.comparison.ComparisonUtility;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

public abstract class AbstractPropertyQueryService<Property, DomainID, DomainModel, DomainModelResponse>
		implements PropertyQueryService<Property, DomainID, DomainModelResponse>
{
	private final FetchPersistenceService<DomainID, DomainModel> persistenceService;
	private final PropertyExtractor<DomainModel, Property> propertyExtractor;
	private final Comparator<Property> propertyComparator;

	private final DomainResponseBuilder<DomainModel, DomainModelResponse> modelMapper;
	private final DomainSecurityPolicy<DomainModel> domainSecurityPolicy;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> findAll(final String propertyName,
																			  final Property property,
																			  final ComparisonType comparisonType)
	{
		return persistenceService.findAll()
								 .stream()
								 .filter(this::isThisModelAccessible)
								 .filter(im -> doesThisModelSatisfyComparisonRequirements(im,
										 model -> propertyExtractor.extractProperty(propertyName, model),
										 property, comparisonType))
								 .map(this::identifiedModel)
								 .toList();
	}


	private boolean isThisModelAccessible(final IdentifiedModel<?, DomainModel> identifiedModel)
	{
		return domainSecurityPolicy.isAccessAllowed(identifiedModel.model());
	}

	private boolean doesThisModelSatisfyComparisonRequirements(final IdentifiedModel<?, DomainModel> domainModel,
															   final Function<DomainModel, Property> propertyExtractor,
															   final Property property,
															   final ComparisonType comparisonType)

	{
		return ComparisonUtility.doesThisValueSatisfyTheComparison(propertyExtractor.apply(domainModel.model()),
				property, comparisonType, propertyComparator);
	}

	private IdentifiedModel<DomainID, DomainModelResponse> identifiedModel(
			final IdentifiedModel<DomainID, DomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), modelMapper.toResponse(domainModel.model()));
	}

	protected AbstractPropertyQueryService(final FetchPersistenceService<DomainID, DomainModel> persistenceService,
										   final PropertyExtractor<DomainModel, Property> propertyExtractor,
										   final Comparator<Property> propertyComparator,
										   final DomainResponseBuilder<DomainModel, DomainModelResponse> modelMapper,
										   final DomainSecurityPolicy<DomainModel> domainSecurityPolicy)
	{
		this.persistenceService = persistenceService;
		this.propertyExtractor = propertyExtractor;
		this.propertyComparator = propertyComparator;
		this.modelMapper = modelMapper;
		this.domainSecurityPolicy = domainSecurityPolicy;
	}
}