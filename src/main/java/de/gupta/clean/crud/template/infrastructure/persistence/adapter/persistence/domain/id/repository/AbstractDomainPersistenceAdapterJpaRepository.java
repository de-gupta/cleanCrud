package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.repository;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.UnexpectedResourceException;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.DomainPersistenceAdapterModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractDomainPersistenceAdapterJpaRepository<DomainID, PersistenceID,
		ConcreteDomainPersistenceModel extends DomainPersistenceAdapterModel<DomainID, PersistenceID>>
		implements DomainPersistenceAdapterRepository<DomainID, PersistenceID, ConcreteDomainPersistenceModel>
{
	private final JpaRepository<ConcreteDomainPersistenceModel, ?> jpaRepository;

	@Override
	public ConcreteDomainPersistenceModel save(final ConcreteDomainPersistenceModel model)
	{
		return castDown(model).map(jpaRepository::save).orElseThrow(() -> UnexpectedResourceException.withMessage(
				"Could not save the unexpected model: " + model + " of type: " + model.getClass().getName()));
	}

	@Override
	public void saveAll(
			final Collection<ConcreteDomainPersistenceModel> models)
	{
		var castedModels = models.stream().map(this::castDownOrThrow).toList();
		jpaRepository.saveAll(castedModels);
	}

	@Override
	public Optional<ConcreteDomainPersistenceModel> findByDomainID(
			final DomainID domainID)
	{
		return findOneByDomainID(domainID);
	}

	@Override
	public Optional<ConcreteDomainPersistenceModel> findByPersistenceID(
			final PersistenceID persistenceID)
	{
		return findOneByPersistenceID(persistenceID);
	}

	protected abstract Optional<ConcreteDomainPersistenceModel> findOneByPersistenceID(PersistenceID persistenceID);

	protected abstract Optional<ConcreteDomainPersistenceModel> findOneByDomainID(DomainID domainID);

	@SuppressWarnings("unchecked")
	private ConcreteDomainPersistenceModel castDownOrThrow(
			final DomainPersistenceAdapterModel<DomainID, PersistenceID> model)
	{
		try
		{
			return (ConcreteDomainPersistenceModel) model;
		}
		catch (ClassCastException e)
		{
			throw UnexpectedResourceException.withMessage(
					"Could not cast down the unexpected model: " + model + " of type: " + model.getClass().getName());
		}
	}

	@SuppressWarnings("unchecked")
	private Optional<ConcreteDomainPersistenceModel> castDown(final DomainPersistenceAdapterModel<DomainID,
			PersistenceID> model)
	{
		try
		{
			return Optional.of((ConcreteDomainPersistenceModel) model);
		}
		catch (ClassCastException e)
		{
			return Optional.empty();
		}
	}

	protected AbstractDomainPersistenceAdapterJpaRepository(
			final JpaRepository<ConcreteDomainPersistenceModel, ?> jpaRepository)
	{
		this.jpaRepository = jpaRepository;
	}
}