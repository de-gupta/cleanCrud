package de.gupta.clean.crud.template.useCases.crud.fetch.api.web;

import de.gupta.clean.crud.template.useCases.crud.fetch.facade.FetchServiceFacade;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

public abstract class AbstractSpringRestFetchController<WebModelResponse, WebModelID>
		implements SpringRestFetchController<WebModelResponse, WebModelID>
{
	private final FetchServiceFacade<WebModelResponse, WebModelID> service;

	@Override
	public ResponseEntity<Page<WebModelResponse>> findAll(Pageable pageable)
	{
		return ResponseEntity.ok(service.findAll(pageable));
	}

	@Override
	public ResponseEntity<WebModelResponse> findById(@PathVariable("id") @Valid final WebModelID id)
	{
		return ResponseEntity.ok(service.findById(id));
	}

	@Override
	public ResponseEntity<Map<WebModelID, WebModelResponse>> findAllByIds(
			@RequestParam("ids") @Valid final Set<WebModelID> ids)
	{
		return ResponseEntity.ok(service.findByIds(ids));
	}

	protected AbstractSpringRestFetchController(
			final FetchServiceFacade<WebModelResponse, WebModelID> service)
	{
		this.service = service;
	}
}