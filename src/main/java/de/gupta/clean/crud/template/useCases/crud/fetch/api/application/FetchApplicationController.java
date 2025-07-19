package de.gupta.clean.crud.template.useCases.crud.fetch.api.application;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FetchApplicationController<WebModelResponse, WebModelID>
{
	Collection<WebModelResponse> findAll();

	WebModelResponse findById(final WebModelID id);

	Map<WebModelID, WebModelResponse> findAllByIds(final Set<WebModelID> ids);
}