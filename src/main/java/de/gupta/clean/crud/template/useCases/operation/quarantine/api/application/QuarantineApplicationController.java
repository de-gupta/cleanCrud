package de.gupta.clean.crud.template.useCases.operation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.PayloadReplayInputs;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineId;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineRecord;

import java.util.Collection;

public interface QuarantineApplicationController<P extends PayloadReplayInputs>
{
	default Collection<QuarantineRecord<P>> findOpen()
	{
		return findOpen(100);
	}

	Collection<QuarantineRecord<P>> findOpen(int limit);

	QuarantineRecord<P> findById(QuarantineId quarantineId);

	QuarantineRecord<P> dismiss(QuarantineId quarantineId);

	QuarantineRecord<P> replay(QuarantineId quarantineId);
}
