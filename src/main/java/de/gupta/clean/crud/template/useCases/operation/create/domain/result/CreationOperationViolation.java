package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import java.util.Objects;

public record CreationOperationViolation(CreationOperationViolationKind kind, String message)
{
	public static CreationOperationViolation access(final String message)
	{
		return new CreationOperationViolation(CreationOperationViolationKind.ACCESS, message);
	}

	public static CreationOperationViolation core(final String message)
	{
		return new CreationOperationViolation(CreationOperationViolationKind.CORE, message);
	}

	public static CreationOperationViolation invariant(final String message)
	{
		return new CreationOperationViolation(CreationOperationViolationKind.INVARIANT, message);
	}

	public static CreationOperationViolation externalConsistency(final String message)
	{
		return new CreationOperationViolation(CreationOperationViolationKind.EXTERNAL_CONSISTENCY, message);
	}

	public CreationOperationViolation
	{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
	}
}