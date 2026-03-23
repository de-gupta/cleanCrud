package de.gupta.clean.crud.template.useCases.crud.delete.api.application;

import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;

import java.util.Collection;

public interface DeleteApplicationController<WebModelID>
{
	void deleteById(final WebModelID id);

	default void deleteAllById(final Collection<WebModelID> ids)
	{
		deleteAllById(ids, BulkOperationMode.ALL_OR_NOTHING);
	}

	void deleteAllById(final Collection<WebModelID> ids, final BulkOperationMode mode);
}