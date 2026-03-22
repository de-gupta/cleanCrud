package de.gupta.clean.crud.template.useCases.crud.delete.facade;

import java.util.Collection;

public interface DeleteServiceFacade<APIModelID>
{
	void deleteById(final APIModelID id);

	void deleteAllById(final Collection<APIModelID> ids);
}