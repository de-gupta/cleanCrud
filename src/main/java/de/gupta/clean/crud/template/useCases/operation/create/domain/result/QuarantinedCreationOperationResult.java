package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record QuarantinedCreationOperationResult<Model>(
		Collection<CreationOperationViolation> blockingViolations,
		Collection<CreationOperationViolation> toleratedViolations,
		Optional<String> quarantineReference) implements CreationOperationResult<Model>
{
	public QuarantinedCreationOperationResult
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