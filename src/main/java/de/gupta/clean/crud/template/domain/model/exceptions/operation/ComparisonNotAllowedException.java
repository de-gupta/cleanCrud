package de.gupta.clean.crud.template.domain.model.exceptions.operation;

public class ComparisonNotAllowedException extends RuntimeException
{
	public static ComparisonNotAllowedException withMessage(String message)
	{
		return new ComparisonNotAllowedException(message);
	}

	private ComparisonNotAllowedException(String message)
	{
		super(message);
	}
}