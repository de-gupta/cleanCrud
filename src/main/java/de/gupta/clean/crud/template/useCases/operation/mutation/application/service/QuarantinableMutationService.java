package de.gupta.clean.crud.template.useCases.operation.mutation.application.service;

import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.MutationQuarantineReplayGateway;

public interface QuarantinableMutationService<DomainId, DomainModel>
		extends MutationService<DomainId, DomainModel>, MutationQuarantineReplayGateway
{
}
