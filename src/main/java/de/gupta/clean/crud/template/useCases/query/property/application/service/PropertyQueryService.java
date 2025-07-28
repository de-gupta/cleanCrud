package de.gupta.clean.crud.template.useCases.query.property.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public interface PropertyQueryService<Property, DomainID, DomainModelResponse>
{
	Collection<IdentifiedModel<DomainID, DomainModelResponse>> findAll(final String propertyName,
																	   final Property property,
																	   final ComparisonType comparisonType);
}