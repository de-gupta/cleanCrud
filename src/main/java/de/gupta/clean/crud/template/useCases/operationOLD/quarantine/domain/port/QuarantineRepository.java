package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.port;

import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.PayloadReplayInputs;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineId;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineRecord;

public interface QuarantineRepository<P extends PayloadReplayInputs>
		extends QuarantineRepositoryPort<QuarantineId, QuarantineRecord<P>>
{
}