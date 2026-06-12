package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.ViolationKind;

import java.util.Optional;

public record OperationPolicyViolationResponse(
		ViolationKind kind,
		String message,
		Optional<InvariantSeverity> severity)
{
}