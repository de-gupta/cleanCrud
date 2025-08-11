package de.gupta.clean.crud.template.domain.service.query;

@FunctionalInterface
public interface PropertyExtractor<DomainModel, Property>
{
	Property extractProperty(final String propertyName, final DomainModel domainModel);
}