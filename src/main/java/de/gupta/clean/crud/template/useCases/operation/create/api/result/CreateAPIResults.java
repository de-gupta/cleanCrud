package de.gupta.clean.crud.template.useCases.operation.create.api.result;

public final class CreateAPIResults
{
	public static <Model> CreateAPIResult<Model> created(final Model createdModel)
	{
		return new CreatedCreateAPIResult<>(createdModel);
	}

	public static <Model> CreateAPIResult<Model> rejected()
	{
		return new RejectedCreateAPIResult<>();
	}

	public static <Model> CreateAPIResult<Model> quarantined()
	{
		return new QuarantinedCreateAPIResult<>();
	}

	private CreateAPIResults()
	{
	}
}
