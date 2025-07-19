package de.gupta.clean.crud.template.useCases.crud.delete.application.service;

public interface DeleteService<DomainID>
{
	void deleteById(final DomainID id);
}