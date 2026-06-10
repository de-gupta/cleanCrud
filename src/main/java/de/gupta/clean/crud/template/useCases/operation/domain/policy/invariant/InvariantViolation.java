package de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant;

import java.util.Objects;

public record InvariantViolation(
		String message,
		InvariantSeverity severity)
{
	public static InvariantViolation hard(final String message)
	{
		return new InvariantViolation(message, InvariantSeverity.HARD);
	}

	public static InvariantViolation soft(final String message)
	{
		return new InvariantViolation(message, InvariantSeverity.SOFT);
	}

	public InvariantViolation
	{
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(severity, "severity");
	}
}
