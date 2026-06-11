package de.gupta.clean.crud.template.useCases.operation.mutation.application.service;

import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineReplayGateway;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.MutationReplayData;

public interface QuarantinableMutationService<DomainId, DomainModel>
		extends MutationService<DomainId, DomainModel>, QuarantineReplayGateway<MutationReplayData>
{
}
