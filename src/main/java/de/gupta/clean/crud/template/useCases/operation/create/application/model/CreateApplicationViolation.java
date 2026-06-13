package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import java.util.Objects;

public record CreateApplicationViolation(CreateApplicationViolationKind kind, String message)
{
	public CreateApplicationViolation
	{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
	}
}