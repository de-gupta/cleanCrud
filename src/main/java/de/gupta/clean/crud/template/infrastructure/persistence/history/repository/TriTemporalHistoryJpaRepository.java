package de.gupta.clean.crud.template.infrastructure.persistence.history.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface TriTemporalHistoryJpaRepository<EntityID, HistoryModel extends TriTemporalHistoryModel<EntityID>>
		extends JpaRepository<HistoryModel, UUID>
{
	Collection<HistoryModel> findAllByEntityIDInAndValidFromIsBeforeAndValidToIsAfter(
			Collection<EntityID> entityIDs,
			Instant validFrom,
			Instant validTo);
}
