package de.gupta.clean.crud.template.useCases.crud.update.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;

public interface UpdateApplicationController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	void putAtId(final WebModelID id, final WebModelCreate model);

	WebModelResponse updateById(final WebModelID id, final WebModelUpdatePatch model);

	Collection<WebModelResponse> updateAllById(
			final Collection<IdentifiedModel<WebModelID, WebModelUpdatePatch>> models);
}