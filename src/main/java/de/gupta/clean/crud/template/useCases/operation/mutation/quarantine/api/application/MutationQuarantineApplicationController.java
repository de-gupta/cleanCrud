package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;

import java.util.Collection;

public interface MutationQuarantineApplicationController
{
	default Collection<MutationQuarantineRecord> findOpen()
	{
		return findOpen(100);
	}

	Collection<MutationQuarantineRecord> findOpen(int limit);

	MutationQuarantineRecord findById(MutationQuarantineId quarantineId);

	MutationQuarantineRecord dismiss(MutationQuarantineId quarantineId);

	MutationQuarantineRecord replay(MutationQuarantineId quarantineId);
}
