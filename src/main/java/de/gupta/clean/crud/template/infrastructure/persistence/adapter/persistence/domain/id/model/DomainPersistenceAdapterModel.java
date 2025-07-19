package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model;

import de.gupta.clean.crud.template.domain.model.builder.ModelBuilder;
import de.gupta.clean.crud.template.domain.model.validation.Validatable;

import java.time.Instant;

public interface DomainPersistenceAdapterModel<DomainID, PersistenceID> extends Validatable
{
	DomainID domainID();

	PersistenceID persistenceID();

	Instant transactionTime();

	void setTransactionTime(final Instant transactionTime);

	Instant decisionTime();

	Instant validFrom();

	Instant validTo();

	void setValidTo(final Instant validTo);

	@Override
	default void validate()
	{
		// TODO
//		Validator<DomainPersistenceAdapterModel<DomainID, PersistenceID>> validator =
//				DomainPersistenceAdapterValidatorFactory.createValidator();
//
//		validator.validate(this);
	}

	interface Builder<DomainID, PersistenceID, T extends DomainPersistenceAdapterModel<DomainID, PersistenceID>>
			extends ModelBuilder<T>
	{
		Builder<DomainID, PersistenceID, T> withDomainID(final DomainID domainID);

		Builder<DomainID, PersistenceID, T> withPersistenceID(final PersistenceID persistenceID);

		Builder<DomainID, PersistenceID, T> withDecisionTime(final Instant decisionTime);

		Builder<DomainID, PersistenceID, T> withValidFrom(final Instant validFrom);

		Builder<DomainID, PersistenceID, T> withValidTo(final Instant validTo);
	}
}