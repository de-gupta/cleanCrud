package de.gupta.clean.crud.template.useCases.incantation.domain.handler;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.ApplicationIncantationPayload;

import java.util.Collection;
import java.util.Optional;

public interface IncantationHandlerRegistry<DomainModelCreate>
{
	static <DomainModelCreate> IncantationHandlerRegistry<DomainModelCreate> of(
			final Collection<RegisteredIncantationHandler<DomainModelCreate, ?>> handlers)
	{
		return new DefaultIncantationHandlerRegistry<>(handlers);
	}

	Optional<RegisteredIncantationHandler<DomainModelCreate, ?>> findHandlerFor(
			Class<? extends ApplicationIncantationPayload> payloadType);
}
