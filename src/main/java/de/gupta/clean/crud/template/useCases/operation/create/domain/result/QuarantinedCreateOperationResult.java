package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationContext;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record QuarantinedCreateOperationResult<Model>(
		CreateOperationContext context,
		Collection<CreationOperationViolation> blockingViolations,
		Collection<CreationOperationViolation> toleratedViolations,
		Optional<String> quarantineReference) implements CreateOperationResult<Model>
{
	public QuarantinedCreateOperationResult
	{
		Objects.requireNonNull(context, "context");
		blockingViolations = List.copyOf(Objects.requireNonNull(blockingViolations, "blockingViolations"));
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
		quarantineReference = Optional.ofNullable(quarantineReference).orElse(Optional.empty());
	}

	@Override
	public CreateOperationStatus status()
	{
		return CreateOperationStatus.QUARANTINED;
	}
}