package de.gupta.clean.crud.template.useCases.crud.update.api.web;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.update.facade.UpdateServiceFacade;
import org.springframework.http.ResponseEntity;

import java.util.Collection;

public abstract class AbstractSpringRestUpdateController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
		implements SpringRestUpdateController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	private final UpdateServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service;

	@Override
	public ResponseEntity<Void> putAtId(final WebModelID id, final WebModelCreate model)
	{
		service.putAtId(id, model);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<WebModelResponse> updateById(final WebModelID id, final WebModelUpdatePatch model)
	{
		return ResponseEntity.ok(service.updateById(id, model));
	}

	@Override
	public ResponseEntity<Collection<WebModelResponse>> updateAllById(
			final Collection<IdentifiedModel<WebModelID, WebModelUpdatePatch>> models)
	{
		return ResponseEntity.ok(service.updateAllById(models));
	}

	protected AbstractSpringRestUpdateController(
			final UpdateServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service)
	{
		this.service = service;
	}
}