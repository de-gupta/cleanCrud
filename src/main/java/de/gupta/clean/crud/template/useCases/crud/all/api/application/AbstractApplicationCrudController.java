package de.gupta.clean.crud.template.useCases.crud.all.api.application;

import de.gupta.clean.crud.template.useCases.crud.all.facade.CrudServiceFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

@Deprecated
public abstract class AbstractApplicationCrudController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
		implements
		ApplicationCrudController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	private final CrudServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service;

	@Override
	public Page<WebModelResponse> findAll(Pageable pageable)
	{
		return service.findAll(pageable);
	}

	@Override
	public WebModelResponse findById(final WebModelID id)
	{
		return service.findById(id);
	}

	@Override
	public WebModelResponse save(final WebModelCreate model)
	{
		return service.save(model);
	}

	@Override
	public Collection<WebModelResponse> saveAll(final Collection<WebModelCreate> models)
	{
		return service.saveAll(models);
	}

	@Override
	public void putAtId(final WebModelID id, final WebModelCreate model)
	{
		service.putAtId(id, model);
	}

	@Override
	public WebModelResponse updateById(final WebModelID id, final WebModelUpdatePatch model)
	{
		return service.updateById(id, model);
	}

	@Override
	public void deleteById(final WebModelID id)
	{
		service.deleteById(id);
	}

	protected AbstractApplicationCrudController(
			final CrudServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service)
	{
		this.service = service;
	}
}