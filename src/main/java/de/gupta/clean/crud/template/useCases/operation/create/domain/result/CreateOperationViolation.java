package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import java.util.Objects;

public record CreateOperationViolation(CreateOperationViolationKind kind, String message)
{
	public static CreateOperationViolation access(final String message)
	{
		return new CreateOperationViolation(CreateOperationViolationKind.ACCESS, message);
	}

	public static CreateOperationViolation core(final String message)
	{
		return new CreateOperationViolation(CreateOperationViolationKind.CORE, message);
	}

	public static CreateOperationViolation invariant(final String message)
	{
		return new CreateOperationViolation(CreateOperationViolationKind.INVARIANT, message);
	}

	public static CreateOperationViolation externalConsistency(final String message)
	{
		return new CreateOperationViolation(CreateOperationViolationKind.EXTERNAL_CONSISTENCY, message);
	}

	public CreateOperationViolation
	{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
	}
}