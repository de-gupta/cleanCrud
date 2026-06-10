package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;

import java.util.Collection;

public interface CreationQuarantineApplicationController
{
	default Collection<CreationQuarantineRecord> findOpen()
	{
		return findOpen(100);
	}

	Collection<CreationQuarantineRecord> findOpen(int limit);

	CreationQuarantineRecord findById(CreationQuarantineId quarantineId);

	CreationQuarantineRecord dismiss(CreationQuarantineId quarantineId);

	CreationQuarantineRecord replay(CreationQuarantineId quarantineId);
}
