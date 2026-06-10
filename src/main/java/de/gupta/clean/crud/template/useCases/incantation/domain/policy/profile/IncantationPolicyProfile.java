package de.gupta.clean.crud.template.useCases.incantation.domain.policy.profile;

import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationViolationHandling;

import java.util.Objects;

public record IncantationPolicyProfile(
		IncantationViolationHandling accessViolationHandling,
		IncantationViolationHandling creationViolationHandling,
		IncantationViolationHandling hardInvariantViolationHandling,
		IncantationViolationHandling softInvariantViolationHandling,
		IncantationViolationHandling externalConsistencyViolationHandling)
{
	public static IncantationPolicyProfile userIntent()
	{
		return new IncantationPolicyProfile(
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT);
	}

	public static IncantationPolicyProfile internalCommand()
	{
		return new IncantationPolicyProfile(
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.ALLOW,
				IncantationViolationHandling.REJECT);
	}

	public static IncantationPolicyProfile authoritativeExternalEvent()
	{
		return new IncantationPolicyProfile(
				IncantationViolationHandling.ALLOW,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.QUARANTINE,
				IncantationViolationHandling.ALLOW,
				IncantationViolationHandling.QUARANTINE);
	}

	public static IncantationPolicyProfile rejectingAll()
	{
		return new IncantationPolicyProfile(
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT,
				IncantationViolationHandling.REJECT);
	}

	public IncantationPolicyProfile
	{
		Objects.requireNonNull(accessViolationHandling, "accessViolationHandling");
		Objects.requireNonNull(creationViolationHandling, "creationViolationHandling");
		Objects.requireNonNull(hardInvariantViolationHandling, "hardInvariantViolationHandling");
		Objects.requireNonNull(softInvariantViolationHandling, "softInvariantViolationHandling");
		Objects.requireNonNull(externalConsistencyViolationHandling, "externalConsistencyViolationHandling");
	}
}
