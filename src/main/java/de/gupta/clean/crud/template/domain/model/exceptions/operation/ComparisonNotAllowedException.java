package de.gupta.clean.crud.template.domain.model.exceptions.operation;

import java.util.function.Supplier;

public class ComparisonNotAllowedException extends RuntimeException
{
	public static Supplier<ComparisonNotAllowedException> forMessage(String message)
	{
		return () -> withMessage(message);
	}

	public static ComparisonNotAllowedException withMessage(String message)
	{
		return new ComparisonNotAllowedException(message);
	}

	private ComparisonNotAllowedException(String message)
	{
		super(message);
	}
}