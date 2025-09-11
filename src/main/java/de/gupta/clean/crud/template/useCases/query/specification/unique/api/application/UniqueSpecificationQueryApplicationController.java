package de.gupta.clean.crud.template.useCases.query.specification.unique.api.application;

import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Optional;

public interface UniqueSpecificationQueryApplicationController<APIModelResponse>
{
	Optional<APIModelResponse> queryUniqueBy(final FilterSpecification filterSpecification);
}