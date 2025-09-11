package de.gupta.clean.crud.template.useCases.query.specification.unique.facade;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.NonUniqueResourceException;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.query.specification.collection.application.service.SpecificationQueryService;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractUniqueSpecificationQueryServiceFacade<DomainID, APIModelResponse, DomainModelResponse>
		implements UniqueSpecificationQueryServiceFacade<APIModelResponse>
{
	private final SpecificationQueryService<DomainID, DomainModelResponse> service;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;
	private final Optional<Supplier<? extends RuntimeException>> exceptionSupplier;

	@Override
	public Optional<APIModelResponse> queryUniqueBy(final FilterSpecification filterSpecification)
	{
		return Unfolding.beckon(service.queryBy(filterSpecification))
						.discern(c -> c.size() <= 1,
								NonUniqueResourceException.forMessage(
										"Expected unique result, but found multiple results"))
						.discern(c -> !c.isEmpty() && exceptionSupplier.isPresent(), exceptionSupplier.get())
						.metamorphose(c -> c.stream().map(responseMapper::mapToAPIModelResponse).findFirst())
						.rescue(Optional.empty());
	}

	protected AbstractUniqueSpecificationQueryServiceFacade(
			final SpecificationQueryService<DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper)
	{
		this(service, responseMapper, Optional.empty());
	}

	protected AbstractUniqueSpecificationQueryServiceFacade(
			final SpecificationQueryService<DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper,
			final Supplier<? extends RuntimeException> exceptionSupplier)
	{
		this(service, responseMapper, Optional.ofNullable(exceptionSupplier));
	}

	protected AbstractUniqueSpecificationQueryServiceFacade(
			final SpecificationQueryService<DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper,
			final Optional<Supplier<? extends RuntimeException>> exceptionSupplier)
	{
		this.service = service;
		this.responseMapper = responseMapper;
		this.exceptionSupplier = exceptionSupplier;
	}
}