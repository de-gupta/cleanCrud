package de.gupta.clean.crud.template.useCases.crud.all.facade.adapter.model;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

public interface CrudAPIDomainModelAdapter<APIModelCreate, APIModelUpdatePatch, APIModelResponse,
		DomainID, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
{
	APIModelResponse mapToWebModelResponse(IdentifiedModel<DomainID, DomainModelResponse> identifiedDomainModel);

	DomainModelCreate mapToDomainModelCreate(APIModelCreate apiModelCreate);

	DomainModelUpdatePatch mapToDomainModelUpdatePatch(APIModelUpdatePatch apiModelUpdatePatch);
}