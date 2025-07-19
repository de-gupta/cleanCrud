package de.gupta.clean.crud.template.useCases.crud.save.api.application;

import java.util.Collection;

public interface SaveApplicationController<WebModelCreate, WebModelResponse>
{
	WebModelResponse save(final WebModelCreate model);

	Collection<WebModelResponse> saveAll(final Collection<WebModelCreate> models);
}