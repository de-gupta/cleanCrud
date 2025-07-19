package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.model;

public interface DomainPersistenceModelAdapter<DomainModel, PersistenceModel>
{
	PersistenceModel toPersistenceModel(DomainModel domainModel);

	DomainModel toDomainModel(PersistenceModel persistenceModel);

	PersistenceModel updatePersistenceModel(PersistenceModel persistenceModel, DomainModel domainModel);
}