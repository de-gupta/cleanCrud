package de.gupta.clean.crud.template.useCases.query.specification.api.web;

import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;
import de.gupta.clean.crud.template.useCases.query.specification.facade.SpecificationQueryServiceFacade;
import org.springframework.http.ResponseEntity;

import java.util.Collection;

public abstract class AbstractSpringRestSpecificationQueryController<APIModelResponse>
		implements SpringRestSpecificationQueryController<APIModelResponse>
{
	private final SpecificationQueryServiceFacade<APIModelResponse> service;
	private final FilterSpecification filterSpecification;

	@Override
	public ResponseEntity<Collection<APIModelResponse>> queryBy()
	{
		return ResponseEntity.ok(service.queryBy(filterSpecification));
	}

	protected AbstractSpringRestSpecificationQueryController(
			final SpecificationQueryServiceFacade<APIModelResponse> service,
			final FilterSpecification filterSpecification)
	{
		this.service = service;
		this.filterSpecification = filterSpecification;
	}
}