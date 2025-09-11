package de.gupta.clean.crud.template.useCases.query.specification.unique.api.application;

import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;
import de.gupta.clean.crud.template.useCases.query.specification.unique.api.behaviour.NotFoundStrategy;
import de.gupta.clean.crud.template.useCases.query.specification.unique.facade.UniqueSpecificationQueryServiceFacade;

import java.util.Optional;

public abstract class AbstractUniqueSpecificationQueryApplicationController<APIModelResponse>
		implements UniqueSpecificationQueryApplicationController<APIModelResponse>
{
	private final UniqueSpecificationQueryServiceFacade<APIModelResponse> service;

	@Override
	public Optional<APIModelResponse> queryUniqueBy(final FilterSpecification filterSpecification)
	{
		return service.queryUniqueBy(filterSpecification, NotFoundStrategy.RETURN_NULL);
	}

	protected AbstractUniqueSpecificationQueryApplicationController(
			final UniqueSpecificationQueryServiceFacade<APIModelResponse> service)
	{
		this.service = service;
	}
}