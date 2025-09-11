package de.gupta.clean.crud.template.useCases.query.specification.unique.facade;

import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;
import de.gupta.clean.crud.template.useCases.query.specification.unique.api.behaviour.NotFoundStrategy;

import java.util.Optional;

public interface UniqueSpecificationQueryServiceFacade<APIModelResponse>
{
	Optional<APIModelResponse> queryUniqueBy(final FilterSpecification filterSpecification,
											 final NotFoundStrategy notFoundStrategy);
}