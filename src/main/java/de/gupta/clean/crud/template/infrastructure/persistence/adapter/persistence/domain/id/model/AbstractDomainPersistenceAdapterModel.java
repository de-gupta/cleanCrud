package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model;

import de.gupta.clean.crud.template.domain.model.builder.AbstractModelBuilder;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(DomainPersistenceAdapterModelEntryListener.class)
public abstract class AbstractDomainPersistenceAdapterModel<DomainID, PersistenceID>
		implements DomainPersistenceAdapterModel<DomainID, PersistenceID>
{
	private static final Instant DEFAULT_END_VALIDITY =
			ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC).toInstant();

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, name = "domain_id")
	private DomainID domainID;

	@Column(nullable = false, name = "persistence_id")
	private PersistenceID persistenceID;

	@Column(nullable = false, updatable = false, name = "transaction_time")
	private Instant transactionTime;

	@Column(nullable = false, name = "decision_time")
	private Instant decisionTime;

	@Column(nullable = false, name = "valid_from")
	private Instant validFrom;

	@Column(nullable = false, name = "valid_to")
	@Check(constraints = "valid_to >= valid_from")
	private Instant validTo;

	@Override
	public DomainID domainID()
	{
		return domainID;
	}

	@Override
	public PersistenceID persistenceID()
	{
		return persistenceID;
	}

	@Override
	public Instant transactionTime()
	{
		return transactionTime;
	}

	@Override
	public void setTransactionTime(final Instant transactionTime)
	{
		this.transactionTime = transactionTime;
	}

	@Override
	public Instant decisionTime()
	{
		return decisionTime;
	}

	@Override
	public Instant validFrom()
	{
		return validFrom;
	}

	@Override
	public Instant validTo()
	{
		return validTo;
	}

	@Override
	public void setValidTo(final Instant validTo)
	{
		this.validTo = validTo;
		validate();
	}

	protected void setDomainID(DomainID domainID)
	{
		this.domainID = domainID;
	}

	protected void setPersistenceID(PersistenceID persistenceID)
	{
		this.persistenceID = persistenceID;
	}

	protected void setDecisionTime(Instant decisionTime)
	{
		this.decisionTime = decisionTime;
	}

	protected void setValidFrom(Instant validFrom)
	{
		this.validFrom = validFrom;
	}

	protected AbstractDomainPersistenceAdapterModel()
	{
	}

	public abstract static class AbstractBuilder<D, P, T extends DomainPersistenceAdapterModel<D, P>>
			extends AbstractModelBuilder<T>
			implements Builder<D, P, T>
	{
		protected final AbstractDomainPersistenceAdapterModel<D, P> model;

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
		public Builder<D, P, T> withDecisionTime(
				final Instant decisionTime)
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
			model.decisionTime = Optional.ofNullable(model.decisionTime).orElse(Instant.now());
			model.validFrom = Optional.ofNullable(model.validFrom).orElse(model.decisionTime);
			model.validTo = Optional.ofNullable(model.validTo).orElse(DEFAULT_END_VALIDITY);
		}

		protected AbstractBuilder(AbstractDomainPersistenceAdapterModel<D, P> model)
		{
			this.model = model;
		}
	}
}