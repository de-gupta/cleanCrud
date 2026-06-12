package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.profile;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.ViolationHandling;

import java.util.Objects;

public record MutationPolicyProfile(
		ViolationHandling accessViolationHandling,
		ViolationHandling coreViolationHandling,
		ViolationHandling hardInvariantViolationHandling,
		ViolationHandling softInvariantViolationHandling,
		ViolationHandling externalConsistencyViolationHandling)
{
	public static MutationPolicyProfile userIntent()
	{
		return rejectingAll();
	}

	public static MutationPolicyProfile internalCommand()
	{
		return new MutationPolicyProfile(
				ViolationHandling.ALLOW,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT);
	}

	public static MutationPolicyProfile authoritativeExternalEvent()
	{
		return new MutationPolicyProfile(
				ViolationHandling.ALLOW,
				ViolationHandling.REJECT,
				ViolationHandling.QUARANTINE,
				ViolationHandling.QUARANTINE,
				ViolationHandling.QUARANTINE);
	}

	public static MutationPolicyProfile rejectingAll()
	{
		return new MutationPolicyProfile(
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT,
				ViolationHandling.REJECT);
	}

	public MutationPolicyProfile
	{
		Objects.requireNonNull(accessViolationHandling, "accessViolationHandling");
		Objects.requireNonNull(coreViolationHandling, "coreViolationHandling");
		Objects.requireNonNull(hardInvariantViolationHandling, "hardInvariantViolationHandling");
		Objects.requireNonNull(softInvariantViolationHandling, "softInvariantViolationHandling");
		Objects.requireNonNull(externalConsistencyViolationHandling, "externalConsistencyViolationHandling");
	}
}