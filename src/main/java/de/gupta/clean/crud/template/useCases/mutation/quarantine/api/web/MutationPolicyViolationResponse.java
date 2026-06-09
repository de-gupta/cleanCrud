package de.gupta.clean.crud.template.useCases.mutation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationViolationKind;

import java.util.Optional;

public record MutationPolicyViolationResponse(
		MutationViolationKind kind,
		String message,
		Optional<InvariantSeverity> severity)
{
}
