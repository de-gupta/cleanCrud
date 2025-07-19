package de.gupta.clean.crud.template.useCases.crud.delete.application.service;

public interface DeletePersistenceService<DomainID>
{
	void deleteById(final DomainID id);
}