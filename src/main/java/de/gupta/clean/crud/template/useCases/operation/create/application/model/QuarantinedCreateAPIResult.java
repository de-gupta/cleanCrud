package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record QuarantinedCreateAPIResult<Model>(
		Collection<CreateAPIViolation> blockingViolations,
		Collection<CreateAPIViolation> toleratedViolations,
		Optional<String> quarantineReference)
		implements CreateAPIResult<Model>
{
	public QuarantinedCreateAPIResult
	{
		blockingViolations = List.copyOf(Objects.requireNonNull(blockingViolations, "blockingViolations"));
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
		quarantineReference = Optional.ofNullable(quarantineReference).orElse(Optional.empty());
	}

	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.QUARANTINED;
	}
}