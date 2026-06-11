package de.gupta.clean.crud.template.useCases.operation.quarantine.domain.port;

import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineId;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineRecord;

public interface QuarantineRepository<P>
		extends QuarantineRepositoryPort<QuarantineId, QuarantineRecord<P>>
{
}
