package de.gupta.clean.crud.template.useCases.query.specification.collection.api.application;

import de.gupta.clean.crud.template.useCases.query.specification.collection.facade.SpecificationQueryServiceFacade;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Collection;

public abstract class AbstractSpecificationQueryApplicationController<APIModelResponse>
		implements SpecificationQueryApplicationController<APIModelResponse>
{
	private final SpecificationQueryServiceFacade<APIModelResponse> service;

	@Override
	public Collection<APIModelResponse> queryBy(final FilterSpecification filterSpecification)
	{
		return service.queryBy(filterSpecification);
	}

	protected AbstractSpecificationQueryApplicationController(
			final SpecificationQueryServiceFacade<APIModelResponse> service)
	{
		this.service = service;
	}
}