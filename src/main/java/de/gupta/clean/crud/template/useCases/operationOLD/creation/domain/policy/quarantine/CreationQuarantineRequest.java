package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.quarantine;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CreationQuarantineRequest(
		OperationSource source,
		List<OperationPolicyViolation> violations,
		Optional<QuarantineId> quarantineId)
{
	public CreationQuarantineRequest(
			final OperationSource source,
			final List<OperationPolicyViolation> violations)
	{
		this(source, violations, Optional.empty());
	}

	public CreationQuarantineRequest
	{
		Objects.requireNonNull(source, "source");
		violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
		quarantineId = Objects.requireNonNull(quarantineId, "quarantineId");
	}

	public CreationQuarantineRequest persistedAs(final QuarantineId quarantineId)
	{
		return new CreationQuarantineRequest(source, violations, Optional.of(quarantineId));
	}
}