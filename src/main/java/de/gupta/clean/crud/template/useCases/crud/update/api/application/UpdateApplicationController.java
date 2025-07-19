package de.gupta.clean.crud.template.useCases.crud.update.api.application;

public interface UpdateApplicationController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	void putAtId(final WebModelID id, final WebModelCreate model);

	WebModelResponse updateById(final WebModelID id, final WebModelUpdatePatch model);
}