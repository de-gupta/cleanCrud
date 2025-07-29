package de.gupta.clean.crud.template.useCases.query.suppliedProperty.facade;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.ComparisonNotAllowedException;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.query.property.application.service.PropertyQueryService;
import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Supplier;

public abstract class AbstractPropertySuppliedQueryServiceFacade<Property, DomainID, DomainModelResponse, APIModelResponse>
		implements PropertySuppliedQueryServiceFacade<APIModelResponse>
{
	private final PropertyQueryService<Property, DomainID, DomainModelResponse> service;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;
	private final Supplier<Property> propertyValueSupplier;
	private final EnumSet<ComparisonType> allowedComparisonTypes;

	@Override
	public Collection<APIModelResponse> queryBySuppliedProperty(final String propertyName,
																final ComparisonType comparisonType)
	{
		return Unfolding.of(comparisonType)
						.discern(allowedComparisonTypes::contains)
						.refold(comparison ->
								service.queryBy(propertyName, propertyValueSupplier.get(), comparison)
									   .stream()
									   .map(responseMapper::mapToAPIModelResponse)
									   .toList()
						)
						.decree(() -> ComparisonNotAllowedException.withMessage(
								"The comparisonType " + comparisonType.name() + " is not allowed for property " + propertyName));
	}

	protected AbstractPropertySuppliedQueryServiceFacade(
			final PropertyQueryService<Property, DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper,
			final Supplier<Property> propertyValueSupplier)
	{
		this(service, responseMapper, propertyValueSupplier, EnumSet.allOf(ComparisonType.class));
	}

	protected AbstractPropertySuppliedQueryServiceFacade(
			final PropertyQueryService<Property, DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper,
			final Supplier<Property> propertyValueSupplier, final EnumSet<ComparisonType> allowedComparisonTypes)
	{
		this.service = service;
		this.responseMapper = responseMapper;
		this.propertyValueSupplier = propertyValueSupplier;
		this.allowedComparisonTypes = allowedComparisonTypes;
	}
}