package de.gupta.clean.crud.template.useCases.incantation.domain.handler;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.ApplicationIncantationPayload;
import de.gupta.clean.crud.template.useCases.incantation.domain.plan.AggregateIncantationPlan;

@FunctionalInterface
public interface AggregateIncantationHandler<DomainModelCreate, IncantationPayload extends ApplicationIncantationPayload>
{
	AggregateIncantationPlan<DomainModelCreate> apply(final IncantationPayload payload);
}
