package de.gupta.clean.crud.template.useCases.mutation.domain.policy;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;

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

	void validate(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
