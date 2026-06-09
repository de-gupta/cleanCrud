package de.gupta.clean.crud.template.useCases.mutation.domain.handler;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;
import de.gupta.clean.crud.template.useCases.mutation.domain.plan.AggregateMutationPlan;

@FunctionalInterface
public interface AggregateMutationHandler<DomainModel, MutationPayload extends ApplicationMutationPayload>
{
	AggregateMutationPlan<DomainModel> apply(final DomainModel currentModel, final MutationPayload payload);
}
