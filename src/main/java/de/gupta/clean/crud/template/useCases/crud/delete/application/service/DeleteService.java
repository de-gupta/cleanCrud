package de.gupta.clean.crud.template.useCases.crud.delete.application.service;

import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;

import java.util.Collection;

public interface DeleteService<DomainID>
{
	void deleteById(final DomainID id);

	default void deleteAllById(final Collection<DomainID> ids)
	{
		deleteAllById(ids, BulkOperationMode.ALL_OR_NOTHING);
	}

	void deleteAllById(final Collection<DomainID> ids, final BulkOperationMode mode);
}