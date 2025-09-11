package de.gupta.clean.crud.template.useCases.query.specification.application.service.adapter;

import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.DomainFilterPipeline;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.LeafFilterSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class FilterSpecificationToDomainFilterAdapterBuilder<DomainModel>
{
	private final Map<Class<? extends LeafFilterSpecification>, Function<LeafFilterSpecification, DomainFilterPipeline<DomainModel>>>
			handlers;
	private final DomainFilterPipeline<DomainModel> defaultPipeline;

	public static <DomainModel> FilterSpecificationToDomainFilterAdapterBuilder<DomainModel> with(
			final DomainFilterPipeline<DomainModel> defaultPipeline)
	{
		return new FilterSpecificationToDomainFilterAdapterBuilder<>(defaultPipeline);
	}

	public <T extends LeafFilterSpecification> FilterSpecificationToDomainFilterAdapterBuilder<DomainModel> registerHandler(
			final Class<T> filterType, final Function<T, DomainFilterPipeline<DomainModel>> handler)
	{
		final Function<LeafFilterSpecification, DomainFilterPipeline<DomainModel>> wrappedHandler =
				spec -> handler.apply(filterType.cast(spec));

		handlers.put(filterType, wrappedHandler);
		return this;
	}

	public FilterSpecificationToDomainFilterAdapter<DomainModel> build()
	{
		final var finalHandlers = Map.copyOf(handlers);

		return filterSpecification ->
				finalHandlers.getOrDefault(filterSpecification.getClass(), _ -> defaultPipeline)
							 .apply(filterSpecification);
	}

	private FilterSpecificationToDomainFilterAdapterBuilder(final DomainFilterPipeline<DomainModel> defaultPipeline)
	{
		this.handlers = new HashMap<>();
		this.defaultPipeline = defaultPipeline;
	}
}