package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

public abstract class AbstractCreateApplicationService<Payload extends CreateOperationPayload>
		implements CreateApplicationService<Payload>
{
}