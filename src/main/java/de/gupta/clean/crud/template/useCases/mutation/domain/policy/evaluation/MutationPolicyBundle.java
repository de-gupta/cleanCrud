package de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.mutation.domain.policy.access.AccessPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.consistency.ExternalConsistencyPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.DomainInvariantPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.profile.MutationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.transition.MutationTransitionPolicy;

import java.util.Objects;

public record MutationPolicyBundle<DomainModel>(
		MutationPolicyProfileResolver profileResolver,
		AccessPolicy<DomainModel> accessPolicy,
		MutationTransitionPolicy<DomainModel> transitionPolicy,
		DomainInvariantPolicy<DomainModel> invariantPolicy,
		ExternalConsistencyPolicy<DomainModel> externalConsistencyPolicy)
{
	public MutationPolicyBundle
	{
		Objects.requireNonNull(profileResolver, "profileResolver");
		Objects.requireNonNull(accessPolicy, "accessPolicy");
		Objects.requireNonNull(transitionPolicy, "transitionPolicy");
		Objects.requireNonNull(invariantPolicy, "invariantPolicy");
		Objects.requireNonNull(externalConsistencyPolicy, "externalConsistencyPolicy");
	}
}
