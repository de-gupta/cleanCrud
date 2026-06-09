package de.gupta.clean.crud.template.useCases.mutation.quarantine.application;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationFamily;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCausationId;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCorrelationId;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;

import java.util.Objects;
import java.util.Optional;

public record MutationQuarantineReplayCommand(
		MutationQuarantineId quarantineId,
		Object domainId,
		ApplicationMutationPayload payload,
		MutationFamily family,
		Optional<MutationCorrelationId> correlationId,
		Optional<MutationCausationId> causationId)
{
	public MutationQuarantineReplayCommand
	{
		Objects.requireNonNull(quarantineId, "quarantineId");
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(causationId, "causationId");
	}
}
