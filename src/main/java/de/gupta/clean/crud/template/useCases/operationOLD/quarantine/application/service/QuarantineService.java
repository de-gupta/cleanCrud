package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service;

import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.PayloadReplayInputs;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineId;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineRecord;

import java.util.Collection;
import java.util.Optional;

public interface QuarantineService<P extends PayloadReplayInputs>
{
	default Collection<QuarantineRecord<P>> findOpen()
	{
		return findOpen(100);
	}

	Collection<QuarantineRecord<P>> findOpen(int limit);

	Optional<QuarantineRecord<P>> findById(QuarantineId quarantineId);

	QuarantineRecord<P> dismiss(QuarantineId quarantineId);

	QuarantineRecord<P> replay(QuarantineId quarantineId);
}