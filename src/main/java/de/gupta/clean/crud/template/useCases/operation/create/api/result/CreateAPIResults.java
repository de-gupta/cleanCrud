package de.gupta.clean.crud.template.useCases.operation.create.api.result;

public final class CreateAPIResults
{
	public static <Model> CreateAPIResult created(final Model createdModel)
	{
		return new CreatedCreateAPIResult<>(createdModel);
	}

	public static CreateAPIResult rejected()
	{
		return new RejectedCreateAPIResult();
	}

	public static CreateAPIResult quarantined()
	{
		return new QuarantinedCreateAPIResult();
	}

	private CreateAPIResults()
	{
	}
}
