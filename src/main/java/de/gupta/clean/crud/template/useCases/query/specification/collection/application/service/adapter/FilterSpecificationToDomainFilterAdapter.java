package de.gupta.clean.crud.template.useCases.query.specification.collection.application.service.adapter;

import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.DomainFilterPipeline;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.CompositeFilterSpecification;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.LeafFilterSpecification;

@FunctionalInterface
public interface FilterSpecificationToDomainFilterAdapter<DomainModel>
{
	DomainFilterPipeline<DomainModel> domainFilterPipeline(final LeafFilterSpecification filterSpecification);

	default DomainFilterPipeline<DomainModel> domainFilterPipeline(
			final CompositeFilterSpecification filterSpecification)
	{
		return filterSpecification.specifications().stream()
								  .map(this::domainFilterPipeline)
								  .reduce((a, b) -> composeFilters(a, b, filterSpecification.operation()))
								  .orElseGet(DomainFilterPipeline::allowing);
	}

	default DomainFilterPipeline<DomainModel> domainFilterPipeline(final FilterSpecification filterSpecification)
	{
		return switch (filterSpecification)
		{
			case LeafFilterSpecification leafFilterSpecification -> domainFilterPipeline(leafFilterSpecification);
			case CompositeFilterSpecification compositeFilterSpecification ->
					domainFilterPipeline(compositeFilterSpecification);
		};
	}

	private static <T> DomainFilterPipeline<T> composeFilters(final DomainFilterPipeline<T> left,
															  final DomainFilterPipeline<T> right,
															  final CompositeFilterSpecification.CompositeFilterOperation operation)
	{
		return model -> operation == CompositeFilterSpecification.CompositeFilterOperation.AND
				? left.allows(model) && right.allows(model)
				: left.allows(model) || right.allows(model);
	}
}