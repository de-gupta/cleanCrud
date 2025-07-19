package de.gupta.clean.crud.template.useCases.crud.save.api.web;

import de.gupta.clean.crud.template.useCases.crud.save.facade.SaveServiceFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collection;

public abstract class AbstractSpringRestSaveController<WebModelCreate, WebModelResponse>
		implements SpringRestSaveController<WebModelCreate, WebModelResponse>
{
	private final SaveServiceFacade<WebModelCreate, WebModelResponse> service;

	@Override
	public ResponseEntity<WebModelResponse> save(final WebModelCreate model)
	{
		return ResponseEntity.status(HttpStatus.CREATED).body(service.save(model));
	}

	@Override
	public ResponseEntity<Collection<WebModelResponse>> saveAll(final Collection<WebModelCreate> models)
	{
		return ResponseEntity.status(HttpStatus.CREATED).body(service.saveAll(models));
	}

	protected AbstractSpringRestSaveController(final SaveServiceFacade<WebModelCreate, WebModelResponse> service)
	{
		this.service = service;
	}
}