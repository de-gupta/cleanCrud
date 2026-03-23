package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model;

import de.gupta.clean.crud.template.domain.model.builder.AbstractModelBuilder;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.AbstractTriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalValidity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;
import java.util.Optional;

@MappedSuperclass
public abstract class AbstractDomainPersistenceAdapterHistoryModel<DomainID, PersistenceID>
		extends AbstractTriTemporalHistoryModel<DomainID>
		implements DomainPersistenceAdapterHistoryModel<DomainID, PersistenceID>
{
	@Column(nullable = false, name = "persistence_id")
	private PersistenceID persistenceID;

	@Override
	public PersistenceID persistenceID()
	{
		return persistenceID;
	}

	protected void setDomainID(final DomainID domainID)
	{
		setEntityID(domainID);
	}

	protected void setPersistenceID(final PersistenceID persistenceID)
	{
		this.persistenceID = persistenceID;
	}

	protected AbstractDomainPersistenceAdapterHistoryModel()
	{
	}

	public abstract static class AbstractBuilder<D, P, T extends DomainPersistenceAdapterHistoryModel<D, P>>
			extends AbstractModelBuilder<T>
			implements Builder<D, P, T>
	{
		protected final AbstractDomainPersistenceAdapterHistoryModel<D, P> model;

		@Override
		public Builder<D, P, T> withDomainID(final D domainID)
		{
			model.setDomainID(domainID);
			return this;
		}

		@Override
		public Builder<D, P, T> withPersistenceID(final P persistenceID)
		{
			model.setPersistenceID(persistenceID);
			return this;
		}

		@Override
		public Builder<D, P, T> withChangeType(final TemporalChangeType changeType)
		{
			model.setChangeType(changeType);
			return this;
		}

		@Override
		public Builder<D, P, T> withDecisionTime(final Instant decisionTime)
		{
			model.setDecisionTime(decisionTime);
			return this;
		}

		@Override
		public Builder<D, P, T> withValidFrom(final Instant validFrom)
		{
			model.setValidFrom(validFrom);
			return this;
		}

		@Override
		public Builder<D, P, T> withValidTo(final Instant validTo)
		{
			model.setValidTo(validTo);
			return this;
		}

		@Override
		protected void setOptionalFieldsToDefaultValues()
		{
			Instant decisionTime = Optional.ofNullable(model.decisionTime()).orElse(Instant.now());
			model.setDecisionTime(decisionTime);
			model.setValidFrom(Optional.ofNullable(model.validFrom()).orElse(decisionTime));
			model.setValidTo(Optional.ofNullable(model.validTo()).orElse(TemporalValidity.defaultEndValidity()));
		}

		protected AbstractBuilder(final AbstractDomainPersistenceAdapterHistoryModel<D, P> model)
		{
			this.model = model;
		}
	}
}