package de.gupta.clean.crud.template.domain.mapping;


public interface CrudDomainModelMapper<DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
{
	DomainModelResponse toResponse(final DomainModel domainModel);

	DomainModel toModel(final DomainModelCreate domainModelCreate);

	DomainModel patchModel(final DomainModel originalModel, final DomainModelUpdatePatch updatePatch);
}