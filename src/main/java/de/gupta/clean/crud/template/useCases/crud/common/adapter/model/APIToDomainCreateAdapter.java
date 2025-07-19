package de.gupta.clean.crud.template.useCases.crud.common.adapter.model;

@FunctionalInterface
public interface APIToDomainCreateAdapter<APIModelCreate, DomainModelCreate>
{
	DomainModelCreate mapToDomainModelCreate(APIModelCreate apiModel);
}