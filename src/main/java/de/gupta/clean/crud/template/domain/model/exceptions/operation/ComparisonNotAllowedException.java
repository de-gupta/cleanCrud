package de.gupta.clean.crud.template.domain.model.exceptions.operation;

import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;

import java.util.function.Supplier;

public class ComparisonNotAllowedException extends DomainException
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
