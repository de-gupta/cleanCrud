package de.gupta.clean.crud.template.useCases.crud.common.adapter.model;

@FunctionalInterface
public interface APIToDomainUpdateAdapter<APIModelUpdatePatch, DomainModelUpdatePatch>
{
	DomainModelUpdatePatch mapToDomainModelUpdatePatch(APIModelUpdatePatch apiModel);
}