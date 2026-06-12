package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.profile;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.ViolationHandling;

import java.util.Objects;

public record CreationPolicyProfile(
		ViolationHandling accessViolationHandling,
		ViolationHandling coreViolationHandling,
		ViolationHandling hardInvariantViolationHandling,
		ViolationHandling softInvariantViolationHandling,
		ViolationHandling externalConsistencyViolationHandling)
{
	public static CreationPolicyProfile userIntent()
	{
		return new CreationPolicyProfile(
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT);
	}

	public static CreationPolicyProfile internalCommand()
	{
		return new CreationPolicyProfile(
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.ALLOW,
				ViolationHandling.REJECT);
	}

	public static CreationPolicyProfile authoritativeExternalEvent()
	{
		return new CreationPolicyProfile(
				ViolationHandling.ALLOW,
				ViolationHandling.REJECT,
				ViolationHandling.QUARANTINE,
				ViolationHandling.ALLOW,
				ViolationHandling.QUARANTINE);
	}

	public static CreationPolicyProfile rejectingAll()
	{
		return new CreationPolicyProfile(
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT);
	}

	public CreationPolicyProfile
	{
		Objects.requireNonNull(accessViolationHandling, "accessViolationHandling");
		Objects.requireNonNull(coreViolationHandling, "coreViolationHandling");
		Objects.requireNonNull(hardInvariantViolationHandling, "hardInvariantViolationHandling");
		Objects.requireNonNull(softInvariantViolationHandling, "softInvariantViolationHandling");
		Objects.requireNonNull(externalConsistencyViolationHandling, "externalConsistencyViolationHandling");
	}
}