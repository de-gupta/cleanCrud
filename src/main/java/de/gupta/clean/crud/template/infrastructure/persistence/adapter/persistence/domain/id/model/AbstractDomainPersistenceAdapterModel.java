package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model;

import de.gupta.clean.crud.template.domain.model.builder.AbstractModelBuilder;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public abstract class AbstractDomainPersistenceAdapterModel<DomainID, PersistenceID>
		implements DomainPersistenceAdapterModel<DomainID, PersistenceID>
{
	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, name = "domain_id")
	private DomainID domainID;

	@Column(nullable = false, name = "persistence_id")
	private PersistenceID persistenceID;

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
	public void setPersistenceID(final PersistenceID persistenceID)
	{
		this.persistenceID = persistenceID;
	}

	protected void setDomainID(DomainID domainID)
	{
		this.domainID = domainID;
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

		protected AbstractBuilder(AbstractDomainPersistenceAdapterModel<D, P> model)
		{
			this.model = model;
		}
	}
}