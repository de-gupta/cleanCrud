package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import java.util.List;
import java.util.Optional;

public final class CreateAPIResults
{
	public static <Model> CreateAPIResult<Model> created(final Model createdModel)
	{
		return created(createdModel, List.of());
	}

	public static <Model> CreateAPIResult<Model> created(
			final Model createdModel,
			final List<CreateAPIViolation> toleratedViolations)
	{
		return new CreatedCreateAPIResult<>(createdModel, toleratedViolations);
	}

	public static <Model> CreateAPIResult<Model> rejected()
	{
		return rejected(List.of());
	}

	public static <Model> CreateAPIResult<Model> rejected(
			final List<CreateAPIViolation> blockingViolations)
	{
		return rejected(blockingViolations, List.of());
	}

	public static <Model> CreateAPIResult<Model> rejected(
			final List<CreateAPIViolation> blockingViolations,
			final List<CreateAPIViolation> toleratedViolations)
	{
		return new RejectedCreateAPIResult<>(blockingViolations, toleratedViolations);
	}

	public static <Model> CreateAPIResult<Model> quarantined()
	{
		return quarantined(List.of());
	}

	public static <Model> CreateAPIResult<Model> quarantined(
			final List<CreateAPIViolation> blockingViolations)
	{
		return quarantined(blockingViolations, List.of(), Optional.empty());
	}

	public static <Model> CreateAPIResult<Model> quarantined(
			final List<CreateAPIViolation> blockingViolations,
			final List<CreateAPIViolation> toleratedViolations)
	{
		return quarantined(blockingViolations, toleratedViolations, Optional.empty());
	}

	public static <Model> CreateAPIResult<Model> quarantined(
			final List<CreateAPIViolation> blockingViolations,
			final List<CreateAPIViolation> toleratedViolations,
			final Optional<String> quarantineReference)
	{
		return new QuarantinedCreateAPIResult<>(blockingViolations, toleratedViolations, quarantineReference);
	}

	private CreateAPIResults()
	{
	}
}
