package de.gupta.clean.crud.template.useCases.crud.delete.api.web;

import de.gupta.clean.crud.template.useCases.crud.delete.facade.DeleteServiceFacade;
import org.springframework.http.ResponseEntity;

import java.util.Collection;

public abstract class AbstractSpringRestDeleteController<WebModelID>
		implements SpringRestDeleteController<WebModelID>
{
	private final DeleteServiceFacade<WebModelID> service;

	@Override
	public ResponseEntity<Void> deleteById(final WebModelID id)
	{
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> deleteAllById(final Collection<WebModelID> ids)
	{
		service.deleteAllById(ids);
		return ResponseEntity.noContent().build();
	}

	protected AbstractSpringRestDeleteController(
			final DeleteServiceFacade<WebModelID> service)
	{
		this.service = service;
	}
}