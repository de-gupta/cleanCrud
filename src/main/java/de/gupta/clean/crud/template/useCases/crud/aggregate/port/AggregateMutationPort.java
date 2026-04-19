package de.gupta.clean.crud.template.useCases.crud.aggregate.port;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

public interface AggregateMutationPort<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch>
{
	IdentifiedModel<DomainId, DomainModel> create(DomainModel domainModel);

	void put(DomainId domainId, DomainModel domainModel);

	IdentifiedModel<DomainId, DomainModel> update(DomainId domainId, DomainModel domainModel);

	void delete(DomainId domainId);
}
