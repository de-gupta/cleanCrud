package de.gupta.clean.crud.template.useCases.operationOLD.mutation.application.service;

import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayGateway;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.MutationReplayData;

public interface QuarantinableMutationService<DomainId, DomainModel>
		extends MutationService<DomainId, DomainModel>, QuarantineReplayGateway<MutationReplayData>
{
}