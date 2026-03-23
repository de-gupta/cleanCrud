package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.DomainPersistenceAdapterHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.JpaTriTemporalHistoryRepositoryAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryJpaRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;

public abstract class AbstractDomainPersistenceAdapterHistoryJpaRepository<DomainID,
		PersistenceID,
		HistoryModel extends DomainPersistenceAdapterHistoryModel<DomainID, PersistenceID>>
		extends JpaTriTemporalHistoryRepositoryAdapter<DomainID, HistoryModel>
		implements TriTemporalHistoryRepository<DomainID, HistoryModel>
{
	protected AbstractDomainPersistenceAdapterHistoryJpaRepository(
			final TriTemporalHistoryJpaRepository<DomainID, HistoryModel> repository)
	{
		super(repository);
	}
}
