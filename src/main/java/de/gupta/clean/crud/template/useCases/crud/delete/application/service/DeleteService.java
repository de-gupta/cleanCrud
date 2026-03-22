package de.gupta.clean.crud.template.useCases.crud.delete.application.service;

import java.util.Collection;

public interface DeleteService<DomainID>
{
	void deleteById(final DomainID id);

	void deleteAllById(final Collection<DomainID> ids);
}