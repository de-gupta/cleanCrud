package de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.incantation.domain.policy.access.IncantationAccessPolicy;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.consistency.IncantationExternalConsistencyPolicy;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.creation.IncantationCreationPolicy;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.invariant.IncantationInvariantPolicy;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.profile.IncantationPolicyProfileResolver;

import java.util.Objects;

public record IncantationPolicyBundle<DomainModel>(
		IncantationPolicyProfileResolver profileResolver,
		IncantationAccessPolicy<DomainModel> accessPolicy,
		IncantationCreationPolicy<DomainModel> creationPolicy,
		IncantationInvariantPolicy<DomainModel> invariantPolicy,
		IncantationExternalConsistencyPolicy<DomainModel> externalConsistencyPolicy)
{
	public IncantationPolicyBundle
	{
		Objects.requireNonNull(profileResolver, "profileResolver");
		Objects.requireNonNull(accessPolicy, "accessPolicy");
		Objects.requireNonNull(creationPolicy, "creationPolicy");
		Objects.requireNonNull(invariantPolicy, "invariantPolicy");
		Objects.requireNonNull(externalConsistencyPolicy, "externalConsistencyPolicy");
	}
}
