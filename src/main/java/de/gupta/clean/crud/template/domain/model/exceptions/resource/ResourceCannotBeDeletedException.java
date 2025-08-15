package de.gupta.clean.crud.template.domain.model.exceptions.resource;

public final class ResourceCannotBeDeletedException extends RuntimeException
{
	public static <ID> ResourceCannotBeDeletedException withId(final ID id)
	{
		final String message = "Resource with id " + id + " cannot be deleted";
		return withMessage(message);
	}

	public static ResourceCannotBeDeletedException withMessage(final String message)
	{
		return new ResourceCannotBeDeletedException(message);
	}

	private ResourceCannotBeDeletedException(final String message)
	{
		super(message);
	}
}