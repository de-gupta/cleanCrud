package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.handler;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.plan.AggregateCreationPlan;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;

@FunctionalInterface
public interface AggregateCreationHandler<DomainModelCreate, CreationPayload extends ApplicationOperationPayload>
{
	AggregateCreationPlan<DomainModelCreate> apply(final CreationPayload payload);
}