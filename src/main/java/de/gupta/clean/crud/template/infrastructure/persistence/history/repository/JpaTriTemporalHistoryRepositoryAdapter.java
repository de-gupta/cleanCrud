package de.gupta.clean.crud.template.infrastructure.persistence.history.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public class JpaTriTemporalHistoryRepositoryAdapter<EntityID, HistoryModel extends TriTemporalHistoryModel<EntityID>>
		implements TriTemporalHistoryRepository<EntityID, HistoryModel>
{
	private final TriTemporalHistoryJpaRepository<EntityID, HistoryModel> repository;

	public JpaTriTemporalHistoryRepositoryAdapter(
			final TriTemporalHistoryJpaRepository<EntityID, HistoryModel> repository)
	{
		this.repository = repository;
	}

	@Override
	public HistoryModel save(final HistoryModel model)
	{
		return repository.save(model);
	}

	@Override
	public void saveAll(final Collection<HistoryModel> models)
	{
		repository.saveAll(models);
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
		return repository.findAllByEntityIDInAndValidFromIsBeforeAndValidToIsAfter(entityIDs, now, now);
	}
}