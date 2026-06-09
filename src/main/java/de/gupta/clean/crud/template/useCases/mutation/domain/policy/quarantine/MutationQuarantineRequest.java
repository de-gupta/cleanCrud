package de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationPolicyViolation;

import java.util.List;
import java.util.Objects;

public record MutationQuarantineRequest(
		MutationSource source,
		List<MutationPolicyViolation> violations)
{
	public MutationQuarantineRequest
	{
		Objects.requireNonNull(source, "source");
		violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
	}
}
