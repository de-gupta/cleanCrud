package de.gupta.clean.crud.template.domain.service.constraints;

public sealed interface ConstraintResult permits ConstraintResult.Satisfied, ConstraintResult.Violated
{
	static ConstraintResult satisfied()
	{
		return new Satisfied();
	}

	static ConstraintResult violated(final String message)
	{
		return new Violated(message);
	}

	boolean isSatisfied();

	default boolean isViolated()
	{
		return !isSatisfied();
	}

	record Satisfied() implements ConstraintResult
	{
		@Override
		public boolean isSatisfied()
		{
			return true;
		}
	}

	record Violated(String message) implements ConstraintResult
	{
		@Override
		public boolean isSatisfied()
		{
			return false;
		}
	}
}