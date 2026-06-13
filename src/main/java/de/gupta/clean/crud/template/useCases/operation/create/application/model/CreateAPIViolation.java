package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import java.util.Objects;

public record CreateAPIViolation(CreateAPIViolationKind kind, String message)
{
	public CreateAPIViolation
	{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
	}
}