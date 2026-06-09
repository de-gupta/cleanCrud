package de.gupta.clean.crud.template.useCases.mutation.quarantine.port.persistence;

import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;

import java.util.Collection;
import java.util.Optional;

public interface MutationQuarantineRepository
{
	MutationQuarantineRecord save(MutationQuarantineRecord record);

	MutationQuarantineRecord update(MutationQuarantineRecord record);

	Optional<MutationQuarantineRecord> findById(MutationQuarantineId quarantineId);

	Collection<MutationQuarantineRecord> findOpen(int limit);
}
