package de.gupta.clean.crud.template.useCases.crud.common.adapter.id;

public interface APIDomainIDAdapter<APIModelID, DomainID>
{
	DomainID mapToDomainID(APIModelID apiModelID);

	APIModelID mapToAPIModelID(DomainID domainID);
}