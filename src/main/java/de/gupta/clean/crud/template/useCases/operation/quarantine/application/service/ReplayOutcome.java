package de.gupta.clean.crud.template.useCases.operation.quarantine.application.service;

import java.util.Objects;
import java.util.Optional;

public record ReplayOutcome(
		boolean applied,
		Optional<String> quarantinedViolationSummary)
{
	public static ReplayOutcome success()
	{
		return new ReplayOutcome(true, Optional.empty());
	}

	public static ReplayOutcome quarantined(final Optional<String> violationSummary)
	{
		return new ReplayOutcome(false, violationSummary);
	}

	public boolean quarantined()
	{
		return !applied;
	}

	public ReplayOutcome
	{
		Objects.requireNonNull(quarantinedViolationSummary, "quarantinedViolationSummary");
	}
}
