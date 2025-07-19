package de.gupta.clean.crud.template.useCases.crud.update.facade;

public interface UpdateServiceFacade<APIModelCreate, APIModelUpdatePatch, APIModelResponse, APIModelID>
{
	void putAtId(final APIModelID id, final APIModelCreate model);

	APIModelResponse updateById(final APIModelID id, final APIModelUpdatePatch updatePatch);
}