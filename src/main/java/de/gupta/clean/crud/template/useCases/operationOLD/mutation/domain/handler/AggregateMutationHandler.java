package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.plan.AggregateMutationPlan;

@FunctionalInterface
public interface AggregateMutationHandler<DomainModel, MutationPayload extends ApplicationOperationPayload>
{
	AggregateMutationPlan<DomainModel> apply(final DomainModel currentModel, final MutationPayload payload);
}