package de.gupta.clean.crud.template.useCases.crud.common.adapter.model;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

@FunctionalInterface
public interface DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse>
{
	APIModelResponse mapToAPIModelResponse(IdentifiedModel<DomainID, DomainModelResponse> domainModel);
}