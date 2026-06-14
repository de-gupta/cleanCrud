package de.gupta.clean.crud.template.useCases.operation.create.domain.execution;

import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

@FunctionalInterface
public interface CreationExecutor<Payload extends CreateOperationPayload, DomainCreateModel, DomainCreatedModel>
{
	DomainCreatedModel create(PreparedCreationAttempt<Payload, DomainCreateModel> preparedAttempt);
}
