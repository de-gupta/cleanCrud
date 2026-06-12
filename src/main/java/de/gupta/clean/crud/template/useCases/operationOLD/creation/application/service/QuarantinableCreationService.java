package de.gupta.clean.crud.template.useCases.operationOLD.creation.application.service;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayGateway;

public interface QuarantinableCreationService<DomainId, DomainModel>
		extends CreationService<DomainId, DomainModel>, QuarantineReplayGateway<ApplicationOperationPayload>
{
}