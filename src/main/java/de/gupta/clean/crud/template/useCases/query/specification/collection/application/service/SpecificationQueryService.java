package de.gupta.clean.crud.template.useCases.query.specification.collection.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Collection;

public interface SpecificationQueryService<DomainID, DomainModelResponse>
{
	Collection<IdentifiedModel<DomainID, DomainModelResponse>> queryBy(final FilterSpecification filterSpecification);
}