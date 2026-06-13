package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record QuarantinedCreateApplicationResult<Model>(
		CreateApplicationResultContext context,
		Collection<CreateApplicationViolation> blockingViolations,
		Collection<CreateApplicationViolation> toleratedViolations,
		Optional<String> quarantineReference)
		implements CreateApplicationResult<Model>
{
	public QuarantinedCreateApplicationResult
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