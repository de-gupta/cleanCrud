package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.port.persistence;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.port.QuarantineRepositoryPort;

import java.util.Collection;
import java.util.Optional;

public interface CreationQuarantineRepository
		extends QuarantineRepositoryPort<CreationQuarantineId, CreationQuarantineRecord>
{
	CreationQuarantineRecord save(CreationQuarantineRecord record);

	CreationQuarantineRecord update(CreationQuarantineRecord record);

	Optional<CreationQuarantineRecord> findById(CreationQuarantineId quarantineId);

	Collection<CreationQuarantineRecord> findOpen(int limit);
}
