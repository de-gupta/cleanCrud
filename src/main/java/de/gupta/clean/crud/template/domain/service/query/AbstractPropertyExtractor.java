package de.gupta.clean.crud.template.domain.service.query;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractPropertyExtractor<DomainModel, Property>
		implements PropertyExtractor<DomainModel, Property>
{
	private final Map<String, Function<DomainModel, Property>> propertyMappers;

	@Override
	public Property extractProperty(final String propertyName, final DomainModel domainModel)
	{
		return Optional.ofNullable(propertyMappers.get(propertyName))
					   .map(f -> f.apply(domainModel))
					   .orElseThrow(() -> new IllegalArgumentException(
							   "No property with name `" + propertyName + "` found"));
	}

	protected AbstractPropertyExtractor(final Map<String, Function<DomainModel, Property>> propertyMappers)
	{
		this.propertyMappers = propertyMappers;
	}
}