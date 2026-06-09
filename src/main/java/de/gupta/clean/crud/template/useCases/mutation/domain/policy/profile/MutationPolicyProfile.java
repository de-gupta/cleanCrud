package de.gupta.clean.crud.template.useCases.mutation.domain.policy.profile;

import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationViolationHandling;

import java.util.Objects;

public record MutationPolicyProfile(
		MutationViolationHandling accessViolationHandling,
		MutationViolationHandling transitionViolationHandling,
		MutationViolationHandling hardInvariantViolationHandling,
		MutationViolationHandling softInvariantViolationHandling,
		MutationViolationHandling externalConsistencyViolationHandling)
{
	public static MutationPolicyProfile userIntent()
	{
		return rejectingAll();
	}

	public static MutationPolicyProfile internalCommand()
	{
		return new MutationPolicyProfile(
				MutationViolationHandling.ALLOW,
				MutationViolationHandling.REJECT,
				MutationViolationHandling.REJECT,
				MutationViolationHandling.REJECT,
				MutationViolationHandling.REJECT);
	}

	public static MutationPolicyProfile authoritativeExternalEvent()
	{
		return new MutationPolicyProfile(
				MutationViolationHandling.ALLOW,
				MutationViolationHandling.REJECT,
				MutationViolationHandling.QUARANTINE,
				MutationViolationHandling.QUARANTINE,
				MutationViolationHandling.QUARANTINE);
	}

	public static MutationPolicyProfile rejectingAll()
	{
		return new MutationPolicyProfile(
				MutationViolationHandling.REJECT,
				MutationViolationHandling.REJECT,
				MutationViolationHandling.REJECT,
				MutationViolationHandling.REJECT,
				MutationViolationHandling.REJECT);
	}

	public MutationPolicyProfile
	{
		Objects.requireNonNull(accessViolationHandling, "accessViolationHandling");
		Objects.requireNonNull(transitionViolationHandling, "transitionViolationHandling");
		Objects.requireNonNull(hardInvariantViolationHandling, "hardInvariantViolationHandling");
		Objects.requireNonNull(softInvariantViolationHandling, "softInvariantViolationHandling");
		Objects.requireNonNull(externalConsistencyViolationHandling, "externalConsistencyViolationHandling");
	}
}
