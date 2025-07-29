package de.gupta.clean.crud.template.domain.mapping.fetch;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

@FunctionalInterface
public interface DomainResponseBuilder<DomainModel, DomainModelResponse>
{
	default <DomainID> IdentifiedModel<DomainID, DomainModelResponse> toResponse(
			final IdentifiedModel<DomainID, DomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), toResponse(domainModel.model()));
	}

	DomainModelResponse toResponse(final DomainModel domainModel);
}