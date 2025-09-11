package de.gupta.clean.crud.template.useCases.query.specification.collection.api.application;

import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Collection;

public interface SpecificationQueryApplicationController<APIModelResponse>
{
	Collection<APIModelResponse> queryBy(final FilterSpecification filterSpecification);
}