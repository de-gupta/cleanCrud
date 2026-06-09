package de.gupta.clean.crud.template.useCases.crud.aggregate.definition;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinitionContract;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.access.AccessPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.consistency.ExternalConsistencyPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.DomainInvariantPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.profile.MutationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.transition.MutationTransitionPolicy;

import java.util.Collection;
import java.util.Optional;

public interface AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
{
	AggregateMutationPort<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> mutationPort();

	AggregateFetchPort<MasterDomainId, MasterDomainModel> fetchPort();

	DomainModelBuilder<MasterDomainModelCreate, MasterDomainModel> createBuilder();

	DomainModelPatcher<MasterDomainModel, MasterDomainModelUpdatePatch> patcher();

	DomainResponseBuilder<MasterDomainModel, MasterDomainModelResponse> responseBuilder();

	InsertionPolicy<MasterDomainModel> insertionPolicy();

	PatchPolicy<MasterDomainModel> patchPolicy();

	DeletionPolicy<MasterDomainModel> deletionPolicy();

	DomainSecurityPolicy<MasterDomainModel> securityPolicy();

	DuplicateDefinition<MasterDomainModel> duplicateDefinition();

	PostCommitMutation<MasterDomainId, MasterDomainModel> postCommitMutation();

	Collection<AggregateRelationshipDefinitionContract<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>> relationshipDefinitions();

	default MutationPolicyProfileResolver mutationPolicyProfileResolver()
	{
		return MutationPolicyProfileResolver.defaultResolver();
	}

	default AccessPolicy<MasterDomainModel> mutationAccessPolicy()
	{
		return (_, beforeModel, afterModel) ->
		{
			if (!securityPolicy().isAccessAllowed(beforeModel))
			{
				return Optional.of("Access not allowed");
			}
			if (!securityPolicy().isAccessAllowed(afterModel))
			{
				return Optional.of("Access not allowed");
			}
			return Optional.empty();
		};
	}

	default MutationTransitionPolicy<MasterDomainModel> mutationTransitionPolicy()
	{
		return (_, beforeModel, afterModel) ->
		{
			try
			{
				patchPolicy().validatePatchAttempt(beforeModel, afterModel);
				return Optional.empty();
			}
			catch (RuntimeException e)
			{
				return Optional.ofNullable(e.getMessage()).or(() -> Optional.of("Mutation transition rejected"));
			}
		};
	}

	default DomainInvariantPolicy<MasterDomainModel> domainInvariantPolicy()
	{
		return DomainInvariantPolicy.allowing();
	}

	default ExternalConsistencyPolicy<MasterDomainModel> externalConsistencyPolicy()
	{
		return ExternalConsistencyPolicy.allowing();
	}
}
