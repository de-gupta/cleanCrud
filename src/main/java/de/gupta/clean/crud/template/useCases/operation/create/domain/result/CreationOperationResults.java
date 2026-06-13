package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import java.util.List;
import java.util.Optional;

public final class CreationOperationResults
{
	public static <Model> CreationOperationResult<Model> created(final Model createdModel)
	{
		return created(createdModel, List.of());
	}

	public static <Model> CreationOperationResult<Model> created(
			final Model createdModel,
			final List<CreationOperationViolation> toleratedViolations)
	{
		return new CreatedCreationOperationResult<>(createdModel, toleratedViolations);
	}

	public static <Model> CreationOperationResult<Model> rejected()
	{
		return rejected(List.of());
	}

	public static <Model> CreationOperationResult<Model> rejected(
			final List<CreationOperationViolation> blockingViolations)
	{
		return rejected(blockingViolations, List.of());
	}

	public static <Model> CreationOperationResult<Model> rejected(
			final List<CreationOperationViolation> blockingViolations,
			final List<CreationOperationViolation> toleratedViolations)
	{
		return new RejectedCreationOperationResult<>(blockingViolations, toleratedViolations);
	}

	public static <Model> CreationOperationResult<Model> quarantined()
	{
		return quarantined(List.of());
	}

	public static <Model> CreationOperationResult<Model> quarantined(
			final List<CreationOperationViolation> blockingViolations)
	{
		return quarantined(blockingViolations, List.of(), Optional.empty());
	}

	public static <Model> CreationOperationResult<Model> quarantined(
			final List<CreationOperationViolation> blockingViolations,
			final List<CreationOperationViolation> toleratedViolations)
	{
		return quarantined(blockingViolations, toleratedViolations, Optional.empty());
	}

	public static <Model> CreationOperationResult<Model> quarantined(
			final List<CreationOperationViolation> blockingViolations,
			final List<CreationOperationViolation> toleratedViolations,
			final Optional<String> quarantineReference)
	{
		return new QuarantinedCreationOperationResult<>(blockingViolations, toleratedViolations, quarantineReference);
	}

	private CreationOperationResults()
	{
	}
}
