package de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MutationQuarantineRequest(
		OperationSource source,
		List<OperationPolicyViolation> violations,
		Optional<MutationQuarantineId> quarantineId)
{
	public MutationQuarantineRequest(
			final OperationSource source,
			final List<OperationPolicyViolation> violations)
	{
		this(source, violations, Optional.empty());
	}

	public MutationQuarantineRequest
	{
		Objects.requireNonNull(source, "source");
		violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
		quarantineId = Objects.requireNonNull(quarantineId, "quarantineId");
	}

	public MutationQuarantineRequest persistedAs(final MutationQuarantineId quarantineId)
	{
		return new MutationQuarantineRequest(source, violations, Optional.of(quarantineId));
	}
}