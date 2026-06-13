package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import java.util.List;
import java.util.Optional;

public final class CreateApplicationResults
{
	public static <Model> CreateApplicationResult<Model> created(
			final CreateApplicationResultContext context,
			final Model createdModel)
	{
		return created(context, createdModel, List.of());
	}

	public static <Model> CreateApplicationResult<Model> created(
			final CreateApplicationResultContext context,
			final Model createdModel,
			final List<CreateApplicationViolation> toleratedViolations)
	{
		return new CreatedCreateApplicationResult<>(context, createdModel, toleratedViolations);
	}

	public static <Model> CreateApplicationResult<Model> rejected(final CreateApplicationResultContext context)
	{
		return rejected(context, List.of());
	}

	public static <Model> CreateApplicationResult<Model> rejected(
			final CreateApplicationResultContext context,
			final List<CreateApplicationViolation> blockingViolations)
	{
		return rejected(context, blockingViolations, List.of());
	}

	public static <Model> CreateApplicationResult<Model> rejected(
			final CreateApplicationResultContext context,
			final List<CreateApplicationViolation> blockingViolations,
			final List<CreateApplicationViolation> toleratedViolations)
	{
		return new RejectedCreateApplicationResult<>(context, blockingViolations, toleratedViolations);
	}

	public static <Model> CreateApplicationResult<Model> quarantined(final CreateApplicationResultContext context)
	{
		return quarantined(context, List.of());
	}

	public static <Model> CreateApplicationResult<Model> quarantined(
			final CreateApplicationResultContext context,
			final List<CreateApplicationViolation> blockingViolations)
	{
		return quarantined(context, blockingViolations, List.of(), Optional.empty());
	}

	public static <Model> CreateApplicationResult<Model> quarantined(
			final CreateApplicationResultContext context,
			final List<CreateApplicationViolation> blockingViolations,
			final List<CreateApplicationViolation> toleratedViolations)
	{
		return quarantined(context, blockingViolations, toleratedViolations, Optional.empty());
	}

	public static <Model> CreateApplicationResult<Model> quarantined(
			final CreateApplicationResultContext context,
			final List<CreateApplicationViolation> blockingViolations,
			final List<CreateApplicationViolation> toleratedViolations,
			final Optional<String> quarantineReference)
	{
		return new QuarantinedCreateApplicationResult<>(
				context,
				blockingViolations,
				toleratedViolations,
				quarantineReference);
	}

	private CreateApplicationResults()
	{
	}
}