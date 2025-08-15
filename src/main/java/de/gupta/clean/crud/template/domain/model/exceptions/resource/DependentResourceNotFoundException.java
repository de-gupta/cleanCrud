package de.gupta.clean.crud.template.domain.model.exceptions.resource;

import java.util.function.Function;

public final class DependentResourceNotFoundException extends RuntimeException
{
	public static DependentResourceNotFoundException withMessage(String message)
	{
		return new DependentResourceNotFoundException(message);
	}

	public static DependentResourceNotFoundException withResourceTypeAndId(Class<?> resourceType, Object resourceId)
	{
		return new DependentResourceNotFoundException(
				"Dependent resource of type " + resourceType.getName() + " with id " + resourceId + " not found");
	}

	public static Function<String, DependentResourceNotFoundException> forResourceType(Class<?> resourceType)
	{
		return s -> DependentResourceNotFoundException.withResourceTypeAndName(resourceType, s);
	}

	public static DependentResourceNotFoundException withResourceTypeAndName(Class<?> resourceType, String resourceName)
	{
		return new DependentResourceNotFoundException(
				"Dependent resource of type " + resourceType.getName() + " with name " + resourceName + " not found");
	}

	private DependentResourceNotFoundException(String message)
	{
		super(message);
	}
}