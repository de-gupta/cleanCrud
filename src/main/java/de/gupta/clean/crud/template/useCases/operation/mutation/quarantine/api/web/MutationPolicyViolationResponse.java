package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationViolationKind;

import java.util.Optional;

public record MutationPolicyViolationResponse(
		MutationViolationKind kind,
		String message,
		Optional<InvariantSeverity> severity)
{
}
