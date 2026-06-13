package de.gupta.clean.crud.template.useCases.operation.create.domain.service;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

public abstract class AbstractCreateApplicationService<Payload extends CreateOperationPayload, DomainModel>
		implements CreateApplicationService<Payload, DomainModel>
{
}