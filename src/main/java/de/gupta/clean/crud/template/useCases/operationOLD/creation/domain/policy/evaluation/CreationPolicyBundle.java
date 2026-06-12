package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.access.CreationAccessPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.consistency.CreationExternalConsistencyPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.creation.CreationPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.invariant.CreationInvariantPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.profile.CreationPolicyProfileResolver;

import java.util.Objects;

public record CreationPolicyBundle<DomainModel>(
		CreationPolicyProfileResolver profileResolver,
		CreationAccessPolicy<DomainModel> accessPolicy,
		CreationPolicy<DomainModel> creationPolicy,
		CreationInvariantPolicy<DomainModel> invariantPolicy,
		CreationExternalConsistencyPolicy<DomainModel> externalConsistencyPolicy)
{
	public CreationPolicyBundle
	{
		Objects.requireNonNull(profileResolver, "profileResolver");
		Objects.requireNonNull(accessPolicy, "accessPolicy");
		Objects.requireNonNull(creationPolicy, "creationPolicy");
		Objects.requireNonNull(invariantPolicy, "invariantPolicy");
		Objects.requireNonNull(externalConsistencyPolicy, "externalConsistencyPolicy");
	}
}