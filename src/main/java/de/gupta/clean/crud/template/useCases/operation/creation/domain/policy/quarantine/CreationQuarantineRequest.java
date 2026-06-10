package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

import java.util.List;
import java.util.Objects;

public record CreationQuarantineRequest(
		OperationSource source,
		List<CreationPolicyViolation> violations)
{
	public CreationQuarantineRequest
	{
		Objects.requireNonNull(source, "source");
		violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
	}
}