package de.gupta.clean.crud.template.useCases.crud.delete.facade;

public interface DeleteServiceFacade<APIModelID>
{
	void deleteById(final APIModelID id);
}