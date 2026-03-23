package de.gupta.clean.crud.template.infrastructure.persistence.history.repository;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.UnexpectedResourceException;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public abstract class AbstractTriTemporalHistoryJpaRepository<EntityID,
		HistoryModel extends TriTemporalHistoryModel<EntityID>,
		ConcreteHistoryModel extends HistoryModel>
		implements TriTemporalHistoryRepository<EntityID, HistoryModel>
{
	private final JpaRepository<ConcreteHistoryModel, ?> jpaRepository;

	@Override
	public HistoryModel save(final HistoryModel model)
	{
		return castDown(model).map(jpaRepository::save).orElseThrow(() -> UnexpectedResourceException.withMessage(
				"Could not save the unexpected history model: " + model + " of type: " + model.getClass().getName()));
	}

	@Override
	public void saveAll(final Collection<HistoryModel> models)
	{
		jpaRepository.saveAll(models.stream().map(this::castDownOrThrow).toList());
	}

	@Override
	public Optional<HistoryModel> findCurrentByEntityID(final EntityID entityID)
	{
		return findCurrentByEntityIDs(java.util.List.of(entityID)).stream().findFirst();
	}

	@Override
	public Collection<HistoryModel> findCurrentByEntityIDs(final Collection<EntityID> entityIDs)
	{
		Instant now = Instant.now();
		return findAllByEntityIDInAndValidFromIsBeforeAndValidToIsAfter(entityIDs, now, now).stream()
																							.map(this::castUp)
																							.toList();
	}

	protected abstract Collection<ConcreteHistoryModel> findAllByEntityIDInAndValidFromIsBeforeAndValidToIsAfter(
			Collection<EntityID> entityIDs, Instant validFrom, Instant validTo);

	private HistoryModel castUp(final ConcreteHistoryModel model)
	{
		return model;
	}

	@SuppressWarnings("unchecked")
	private ConcreteHistoryModel castDownOrThrow(final HistoryModel model)
	{
		try
		{
			return (ConcreteHistoryModel) model;
		}
		catch (ClassCastException e)
		{
			throw UnexpectedResourceException.withMessage(
					"Could not cast down the unexpected history model: " + model + " of type: " +
							model.getClass().getName());
		}
	}

	@SuppressWarnings("unchecked")
	private Optional<ConcreteHistoryModel> castDown(final HistoryModel model)
	{
		try
		{
			return Optional.of((ConcreteHistoryModel) model);
		}
		catch (ClassCastException e)
		{
			return Optional.empty();
		}
	}

	protected AbstractTriTemporalHistoryJpaRepository(final JpaRepository<ConcreteHistoryModel, ?> jpaRepository)
	{
		this.jpaRepository = jpaRepository;
	}
}