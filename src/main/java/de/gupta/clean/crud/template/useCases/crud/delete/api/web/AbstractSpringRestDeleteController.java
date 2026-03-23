package de.gupta.clean.crud.template.useCases.crud.delete.api.web;

import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.crud.delete.facade.DeleteServiceFacade;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collection;

public abstract class AbstractSpringRestDeleteController<WebModelID>
		implements SpringRestDeleteController<WebModelID>
{
	private final DeleteServiceFacade<WebModelID> service;

	@Override
	public ResponseEntity<Void> deleteById(@PathVariable("id") @Valid final WebModelID id)
	{
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> deleteAllById(final Collection<WebModelID> ids, final BulkOperationMode mode)
	{
		service.deleteAllById(ids, mode);
		return ResponseEntity.noContent().build();
	}

	protected AbstractSpringRestDeleteController(
			final DeleteServiceFacade<WebModelID> service)
	{
		this.service = service;
	}
}