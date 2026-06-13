package de.gupta.clean.crud.template.useCases.operation.create.application.internal;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

public abstract class AbstractCreateApplicationService<Payload extends CreateOperationPayload, DomainModel>
		implements CreateApplicationService<Payload, DomainModel>
{
}