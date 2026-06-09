package de.gupta.clean.crud.template.useCases.mutation.aggregate.service;

import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.MutationAccessPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.MutationInvariantPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.SourceAwareMutationPolicy;

public final class AggregateMutationPolicies
{
	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	SourceAwareMutationPolicy<DomainModel> sourceAwarePolicy(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition)
	{
		return SourceAwareMutationPolicy.of(accessPolicy(definition), invariantPolicy(definition));
	}

	private static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationAccessPolicy<DomainModel> accessPolicy(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition)
	{
		return (source, beforeModel, afterModel) ->
		{
			if (source != MutationSource.USER_INTENT)
			{
				return;
			}
			if (!definition.securityPolicy().isAccessAllowed(beforeModel))
			{
				throw AccessDeniedException.withMessage("Access not allowed");
			}
			if (!definition.securityPolicy().isAccessAllowed(afterModel))
			{
				throw AccessDeniedException.withMessage("Access not allowed");
			}
		};
	}

	private static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationInvariantPolicy<DomainModel> invariantPolicy(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition)
	{
		return (_, beforeModel, afterModel) -> definition.patchPolicy().validatePatchAttempt(beforeModel, afterModel);
	}

	private AggregateMutationPolicies()
	{
	}
}
