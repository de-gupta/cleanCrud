package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.port.persistence;

import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.quarantine.port.persistence.QuarantineRepositoryPort;

import java.util.Collection;
import java.util.Optional;

public interface MutationQuarantineRepository
		extends QuarantineRepositoryPort<MutationQuarantineId, MutationQuarantineRecord>
{
	MutationQuarantineRecord save(MutationQuarantineRecord record);

	MutationQuarantineRecord update(MutationQuarantineRecord record);

	Optional<MutationQuarantineRecord> findById(MutationQuarantineId quarantineId);

	Collection<MutationQuarantineRecord> findOpen(int limit);
}
