package de.gupta.clean.crud.template.useCases.operation.create.domain.execution;

import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

@FunctionalInterface
public interface CreateExecutor<DomainCreatePayload extends CreateOperationPayload, DomainModel>
{
	DomainModel create(PreparedCreationAttempt<DomainCreatePayload, DomainModel> preparedAttempt);
}