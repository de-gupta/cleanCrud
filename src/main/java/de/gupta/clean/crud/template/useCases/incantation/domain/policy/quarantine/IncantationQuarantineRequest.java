package de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationPolicyViolation;

import java.util.List;
import java.util.Objects;

public record IncantationQuarantineRequest(
		IncantationSource source,
		List<IncantationPolicyViolation> violations)
{
	public IncantationQuarantineRequest
	{
		Objects.requireNonNull(source, "source");
		violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
	}
}
