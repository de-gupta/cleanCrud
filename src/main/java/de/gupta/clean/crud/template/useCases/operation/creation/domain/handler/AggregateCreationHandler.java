package de.gupta.clean.crud.template.useCases.operation.creation.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.plan.AggregateCreationPlan;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;

@FunctionalInterface
public interface AggregateCreationHandler<DomainModelCreate, CreationPayload extends ApplicationOperationPayload>
{
	AggregateCreationPlan<DomainModelCreate> apply(final CreationPayload payload);
}