package de.gupta.clean.crud.template.useCases.crud.delete.facade;

import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;

import java.util.Collection;

public interface DeleteServiceFacade<APIModelID>
{
	void deleteById(final APIModelID id);

	default void deleteAllById(final Collection<APIModelID> ids)
	{
		deleteAllById(ids, BulkOperationMode.ALL_OR_NOTHING);
	}

	void deleteAllById(final Collection<APIModelID> ids, final BulkOperationMode mode);
}
