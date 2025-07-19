package de.gupta.clean.crud.template.useCases.crud.delete.api.application;

public interface DeleteApplicationController<WebModelID>
{
	void deleteById(final WebModelID id);
}