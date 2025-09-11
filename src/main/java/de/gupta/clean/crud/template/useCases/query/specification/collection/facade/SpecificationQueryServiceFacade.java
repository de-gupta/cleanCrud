package de.gupta.clean.crud.template.useCases.query.specification.collection.facade;

import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Collection;

public interface SpecificationQueryServiceFacade<APIModelResponse>
{
	Collection<APIModelResponse> queryBy(final FilterSpecification filterSpecification);
}