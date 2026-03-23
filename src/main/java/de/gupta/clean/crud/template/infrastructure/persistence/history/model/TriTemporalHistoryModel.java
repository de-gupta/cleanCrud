package de.gupta.clean.crud.template.infrastructure.persistence.history.model;

import de.gupta.clean.crud.template.domain.model.validation.Validatable;

import java.time.Instant;

public interface TriTemporalHistoryModel<EntityID> extends Validatable
{
	EntityID entityID();

	TemporalChangeType changeType();

	Instant transactionTime();

	void setTransactionTime(Instant transactionTime);

	Instant decisionTime();

	Instant validFrom();

	Instant validTo();

	void setValidTo(Instant validTo);

	@Override
	default void validate()
	{
	}
}