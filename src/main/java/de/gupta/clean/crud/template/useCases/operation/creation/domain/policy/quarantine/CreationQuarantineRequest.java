package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CreationQuarantineRequest(
		OperationSource source,
		List<CreationPolicyViolation> violations,
		Optional<CreationQuarantineId> quarantineId)
{
	public CreationQuarantineRequest(
			final OperationSource source,
			final List<CreationPolicyViolation> violations)
	{
		this(source, violations, Optional.empty());
	}

	public CreationQuarantineRequest
	{
		Objects.requireNonNull(source, "source");
		violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
		quarantineId = Objects.requireNonNull(quarantineId, "quarantineId");
	}

	public CreationQuarantineRequest persistedAs(final CreationQuarantineId quarantineId)
	{
		return new CreationQuarantineRequest(source, violations, Optional.of(quarantineId));
	}
}