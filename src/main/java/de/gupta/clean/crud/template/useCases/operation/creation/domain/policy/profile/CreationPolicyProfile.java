package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.profile;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationViolationHandling;

import java.util.Objects;

public record CreationPolicyProfile(
		CreationViolationHandling accessViolationHandling,
		CreationViolationHandling creationViolationHandling,
		CreationViolationHandling hardInvariantViolationHandling,
		CreationViolationHandling softInvariantViolationHandling,
		CreationViolationHandling externalConsistencyViolationHandling)
{
	public static CreationPolicyProfile userIntent()
	{
		return new CreationPolicyProfile(
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT);
	}

	public static CreationPolicyProfile internalCommand()
	{
		return new CreationPolicyProfile(
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.ALLOW,
				CreationViolationHandling.REJECT);
	}

	public static CreationPolicyProfile authoritativeExternalEvent()
	{
		return new CreationPolicyProfile(
				CreationViolationHandling.ALLOW,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.QUARANTINE,
				CreationViolationHandling.ALLOW,
				CreationViolationHandling.QUARANTINE);
	}

	public static CreationPolicyProfile rejectingAll()
	{
		return new CreationPolicyProfile(
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT,
				CreationViolationHandling.REJECT);
	}

	public CreationPolicyProfile
	{
		Objects.requireNonNull(accessViolationHandling, "accessViolationHandling");
		Objects.requireNonNull(creationViolationHandling, "creationViolationHandling");
		Objects.requireNonNull(hardInvariantViolationHandling, "hardInvariantViolationHandling");
		Objects.requireNonNull(softInvariantViolationHandling, "softInvariantViolationHandling");
		Objects.requireNonNull(externalConsistencyViolationHandling, "externalConsistencyViolationHandling");
	}
}
