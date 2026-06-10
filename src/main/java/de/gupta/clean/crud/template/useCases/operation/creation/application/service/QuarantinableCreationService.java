package de.gupta.clean.crud.template.useCases.operation.creation.application.service;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineReplayGateway;

public interface QuarantinableCreationService<DomainId, DomainModel>
		extends CreationService<DomainId, DomainModel>, CreationQuarantineReplayGateway
{
}
