package de.gupta.clean.crud.template.useCases.crud.delete.api.application;

import java.util.Collection;

public interface DeleteApplicationController<WebModelID>
{
	void deleteById(final WebModelID id);

	void deleteAllById(final Collection<WebModelID> ids);
}
