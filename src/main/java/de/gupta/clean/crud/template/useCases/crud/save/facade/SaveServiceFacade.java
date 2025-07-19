package de.gupta.clean.crud.template.useCases.crud.save.facade;

import java.util.Collection;

public interface SaveServiceFacade<APIModelCreate, APIModelResponse>
{
	APIModelResponse save(final APIModelCreate model);

	Collection<APIModelResponse> saveAll(final Collection<APIModelCreate> models);
}