package de.gupta.clean.crud.template.domain.model.exceptions.resource;

import java.io.Serializable;

public final class ResourceNotFoundException extends RuntimeException
{
	public static <ID> ResourceNotFoundException withId(final ID id)
	{
		final String message = "Resource with id " + id + " not found";
		return withMessage(message);
	}

	public static ResourceNotFoundException withMessage(final String message)
	{
		return new ResourceNotFoundException(message);
	}

	public static ResourceNotFoundException withResourceTypeAndId(final Class<?> resource, final Serializable id)
	{
		final String message = "Resource " + resource.getName() + " with id " + id + " not found";
		return withMessage(message);
	}

	public static ResourceNotFoundException withResourceTypeAndName(final Class<?> resource, final String name)
	{
		final String message = "Resource " + resource.getName() + " with name " + name + " not found";
		return withMessage(message);
	}

	private ResourceNotFoundException(final String message)
	{
		super(message);
	}
}