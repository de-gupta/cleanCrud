package de.gupta.clean.crud.template.useCases.operation.creation.application.service;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineReplayGateway;

public interface QuarantinableCreationService<DomainId, DomainModel>
		extends CreationService<DomainId, DomainModel>, QuarantineReplayGateway<ApplicationOperationPayload>
{
}
