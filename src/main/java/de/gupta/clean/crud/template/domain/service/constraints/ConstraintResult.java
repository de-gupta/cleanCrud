package de.gupta.clean.crud.template.domain.service.constraints;

public sealed interface ConstraintResult permits ConstraintResult.Satisfied, ConstraintResult.Violated
{
	static ConstraintResult satisfied()
	{
		return Satisfied.instance();
	}

	static ConstraintResult violated(final String message)
	{
		return Violated.from(message);
	}

	boolean isSatisfied();

	default boolean isViolated()
	{
		return !isSatisfied();
	}

	record Violated(String message) implements ConstraintResult
	{
		static Violated from(final String message)
		{
			return new Violated(message);
		}

		@Override
		public boolean isSatisfied()
		{
			return false;
		}
	}

	final class Satisfied implements ConstraintResult
	{
		private final static Satisfied INSTANCE = new Satisfied();

		static Satisfied instance()
		{
			return INSTANCE;
		}

		@Override
		public boolean isSatisfied()
		{
			return true;
		}

		private Satisfied()
		{
		}
	}
}