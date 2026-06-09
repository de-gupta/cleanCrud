package de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.access.MutationAccessPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.MutationInvariantPolicy;

import java.util.Objects;

@FunctionalInterface
public interface SourceAwareMutationPolicy<DomainModel>
{
	static <DomainModel> SourceAwareMutationPolicy<DomainModel> of(
			final MutationAccessPolicy<DomainModel> accessPolicy,
			final MutationInvariantPolicy<DomainModel> invariantPolicy)
	{
		Objects.requireNonNull(accessPolicy, "accessPolicy");
		Objects.requireNonNull(invariantPolicy, "invariantPolicy");
		return (source, beforeModel, afterModel) ->
		{
			accessPolicy.validateAccess(source, beforeModel, afterModel);
			invariantPolicy.validateInvariant(source, beforeModel, afterModel);
		};
	}

	default MutationPolicyDecision evaluate(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel)
	{
		validate(source, beforeModel, afterModel);
		return MutationPolicyDecision.allow();
	}

	void validate(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
