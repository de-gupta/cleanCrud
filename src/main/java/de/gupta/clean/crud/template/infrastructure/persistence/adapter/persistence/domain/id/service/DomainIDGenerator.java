package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.service;

public interface DomainIDGenerator<DomainID>
{
	DomainID generate();

	DomainID generate(final DomainID domainID);
}