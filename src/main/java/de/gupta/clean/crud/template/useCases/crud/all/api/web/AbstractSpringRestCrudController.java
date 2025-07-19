package de.gupta.clean.crud.template.useCases.crud.all.api.web;

import de.gupta.clean.crud.template.useCases.crud.all.facade.CrudServiceFacade;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;

@Deprecated
public abstract class AbstractSpringRestCrudController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
		implements
		SpringRestCrudController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	private final CrudServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service;

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
	public ResponseEntity<WebModelResponse> save(@RequestBody @Valid final WebModelCreate model)
	{
		return ResponseEntity.status(HttpStatus.CREATED).body(service.save(model));
	}

	@Override
	public ResponseEntity<Collection<WebModelResponse>> saveAll(
			@RequestBody @Valid final Collection<WebModelCreate> models)
	{
		return ResponseEntity.status(HttpStatus.CREATED).body(service.saveAll(models));
	}

	@Override
	public ResponseEntity<Void> putAtId(@PathVariable("id") @Valid final WebModelID id,
										@RequestBody @Valid final WebModelCreate model)
	{
		service.putAtId(id, model);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<WebModelResponse> updateById(@PathVariable("id") @Valid final WebModelID id,
													   @RequestBody @Valid final WebModelUpdatePatch model)
	{
		return ResponseEntity.ok(service.updateById(id, model));
	}

	@Override
	public ResponseEntity<Void> deleteById(@PathVariable("id") @Valid final WebModelID id)
	{
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	protected AbstractSpringRestCrudController(
			final CrudServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service)
	{
		this.service = service;
	}
}