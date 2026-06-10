package de.gupta.clean.crud.template.useCases.incantation.domain.handler;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.ApplicationIncantationPayload;

@FunctionalInterface
public interface IncantationHandler<DomainModelCreate, IncantationPayload extends ApplicationIncantationPayload>
{
	DomainModelCreate apply(final IncantationPayload payload);
}
