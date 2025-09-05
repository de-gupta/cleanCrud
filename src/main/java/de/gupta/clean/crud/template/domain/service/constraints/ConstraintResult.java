package de.gupta.clean.crud.template.domain.service.constraints;

import java.util.Optional;

public record ConstraintResult(boolean constraintSatisfied, Optional<String> message)
{
	public static ConstraintResult satisfied()
	{
		return new ConstraintResult(true, Optional.empty());
	}

	public static ConstraintResult violated(final String message)
	{
		return new ConstraintResult(false, Optional.of(message));
	}
}