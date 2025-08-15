package de.gupta.clean.crud.template.domain.model.exceptions.resource;

import java.io.Serializable;

public final class ResourceAlreadyExistsException extends RuntimeException
{
	public static <ID> ResourceAlreadyExistsException withId(final ID id)
	{
		final String message = "Resource with id " + id + " already exists";
		return withMessage(message);
	}

	public static ResourceAlreadyExistsException withMessage(final String message)
	{
		return new ResourceAlreadyExistsException(message);
	}

	public static ResourceAlreadyExistsException withResourceTypeAndId(final Class<?> resource, final Serializable id)
	{
		final String message = "Resource " + resource.getName() + " with id " + id + " already exists";
		return withMessage(message);
	}

	public static ResourceAlreadyExistsException withResourceTypeAndName(final Class<?> resource, final String name)
	{
		final String message = "Resource " + resource.getName() + " with name " + name + " already exists";
		return withMessage(message);
	}

	private ResourceAlreadyExistsException(final String message)
	{
		super(message);
	}
}