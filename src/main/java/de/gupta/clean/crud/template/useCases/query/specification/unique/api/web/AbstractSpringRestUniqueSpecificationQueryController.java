package de.gupta.clean.crud.template.useCases.query.specification.unique.api.web;

import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;
import de.gupta.clean.crud.template.useCases.query.specification.unique.api.behaviour.NotFoundStrategy;
import de.gupta.clean.crud.template.useCases.query.specification.unique.facade.UniqueSpecificationQueryServiceFacade;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public abstract class AbstractSpringRestUniqueSpecificationQueryController<APIModelResponse>
		implements SpringRestUniqueSpecificationQueryController<APIModelResponse>
{
	private final UniqueSpecificationQueryServiceFacade<APIModelResponse> uniqueSpecificationQueryServiceFacade;
	private final FilterSpecification filterSpecification;

	@Override
	public ResponseEntity<Optional<APIModelResponse>> queryUniqueBy(final NotFoundStrategy notFoundStrategy)
	{
		return ResponseEntity.ok(
				uniqueSpecificationQueryServiceFacade.queryUniqueBy(filterSpecification, notFoundStrategy));
	}

	protected AbstractSpringRestUniqueSpecificationQueryController(
			UniqueSpecificationQueryServiceFacade<APIModelResponse> uniqueSpecificationQueryServiceFacade,
			final FilterSpecification filterSpecification)
	{
		this.uniqueSpecificationQueryServiceFacade = uniqueSpecificationQueryServiceFacade;
		this.filterSpecification = filterSpecification;
	}
}