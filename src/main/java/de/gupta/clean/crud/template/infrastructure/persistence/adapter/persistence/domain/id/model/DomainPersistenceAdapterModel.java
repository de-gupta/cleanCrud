package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model;

import de.gupta.clean.crud.template.domain.model.builder.ModelBuilder;
import de.gupta.clean.crud.template.domain.model.validation.Validatable;

public interface DomainPersistenceAdapterModel<DomainID, PersistenceID> extends Validatable
{
	DomainID domainID();

	PersistenceID persistenceID();

	void setPersistenceID(PersistenceID persistenceID);

	@Override
	default void validate()
	{
	}

	interface Builder<DomainID, PersistenceID, T extends DomainPersistenceAdapterModel<DomainID, PersistenceID>>
			extends ModelBuilder<T>
	{
		Builder<DomainID, PersistenceID, T> withDomainID(final DomainID domainID);

		Builder<DomainID, PersistenceID, T> withPersistenceID(final PersistenceID persistenceID);
	}
}