package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationViolationKind;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.invariant.InvariantSeverity;

import java.util.Optional;

public record CreationPolicyViolationResponse(
		CreationViolationKind kind,
		String message,
		Optional<InvariantSeverity> severity)
{
}
