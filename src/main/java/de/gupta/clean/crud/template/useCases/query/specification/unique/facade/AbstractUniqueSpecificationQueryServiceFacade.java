package de.gupta.clean.crud.template.useCases.query.specification.unique.facade;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.NonUniqueResourceException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.query.specification.collection.application.service.SpecificationQueryService;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;
import de.gupta.clean.crud.template.useCases.query.specification.unique.api.behaviour.NotFoundStrategy;

import java.util.Optional;

public abstract class AbstractUniqueSpecificationQueryServiceFacade<DomainID, APIModelResponse, DomainModelResponse>
		implements UniqueSpecificationQueryServiceFacade<APIModelResponse>
{
	private final SpecificationQueryService<DomainID, DomainModelResponse> service;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;

	@Override
	public Optional<APIModelResponse> queryUniqueBy(final FilterSpecification filterSpecification,
	                                                final NotFoundStrategy notFoundStrategy)
	{
		return Unfolding.beckon(service.queryBy(filterSpecification))
		                .discern(c -> c.size() <= 1,
								NonUniqueResourceException.forMessage(
										"Expected unique result, but found multiple results"))
		                .discern(c -> !c.isEmpty() || notFoundStrategy == NotFoundStrategy.RETURN_NULL,
								ResourceNotFoundException.forMessage("Expected unique result, but found none"))
		                .metamorphose(c -> c.stream().map(responseMapper::mapToAPIModelResponse).findFirst())
		                .infuse(Optional.empty());
	}

	protected AbstractUniqueSpecificationQueryServiceFacade(
			final SpecificationQueryService<DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper)
	{
		this.service = service;
		this.responseMapper = responseMapper;
	}
}