package de.gupta.clean.crud.template.useCases.crud.update.facade;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;

public interface UpdateServiceFacade<APIModelCreate, APIModelUpdatePatch, APIModelResponse, APIModelID>
{
	void putAtId(final APIModelID id, final APIModelCreate model);

	APIModelResponse updateById(final APIModelID id, final APIModelUpdatePatch updatePatch);

	Collection<APIModelResponse> updateAllById(
			final Collection<IdentifiedModel<APIModelID, APIModelUpdatePatch>> models);
}