package de.gupta.clean.crud.template.useCases.query.property.application.service;

@FunctionalInterface
public interface PropertyExtractor<DomainModel, Property>
{
	Property extractProperty(final String propertyName, final DomainModel domainModel);
}