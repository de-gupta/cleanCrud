package de.gupta.clean.crud.template.domain.aggregate.definition;

import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinitionContract;
import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.access.CreationAccessPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.consistency.CreationExternalConsistencyPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.creation.CreationPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.invariant.CreationInvariantPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.profile.CreationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.access.AccessPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.consistency.ExternalConsistencyPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.invariant.DomainInvariantPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.profile.MutationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.transition.MutationTransitionPolicy;

import java.util.Collection;
import java.util.Optional;

public interface AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
{
	AggregateMutationPort<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> mutationPort();

	AggregateFetchPort<MasterDomainId, MasterDomainModel> fetchPort();

	DomainModelBuilder<MasterDomainModelCreate, MasterDomainModel> createBuilder();

	DomainModelPatcher<MasterDomainModel, MasterDomainModelUpdatePatch> patcher();

	DomainResponseBuilder<MasterDomainModel, MasterDomainModelResponse> responseBuilder();

	DeletionPolicy<MasterDomainModel> deletionPolicy();

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

	DomainSecurityPolicy<MasterDomainModel> securityPolicy();

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

	PatchPolicy<MasterDomainModel> patchPolicy();

	default DomainInvariantPolicy<MasterDomainModel> domainInvariantPolicy()
	{
		return DomainInvariantPolicy.allowing();
	}

	default ExternalConsistencyPolicy<MasterDomainModel> externalConsistencyPolicy()
	{
		return ExternalConsistencyPolicy.allowing();
	}

	default CreationPolicyProfileResolver creationPolicyProfileResolver()
	{
		return CreationPolicyProfileResolver.defaultResolver();
	}

	default CreationAccessPolicy<MasterDomainModel> creationAccessPolicy()
	{
		return (_, afterModel) ->
		{
			if (!securityPolicy().isAccessAllowed(afterModel))
			{
				throw new IllegalStateException("Access not allowed");
			}
		};
	}

	default CreationPolicy<MasterDomainModel> creationPolicy()
	{
		return (_, afterModel) ->
		{
			try
			{
				insertionPolicy().validateInsertion(afterModel);
				return Optional.empty();
			}
			catch (RuntimeException e)
			{
				return Optional.ofNullable(e.getMessage()).or(() -> Optional.of("Creation rejected"));
			}
		};
	}

	InsertionPolicy<MasterDomainModel> insertionPolicy();

	default CreationInvariantPolicy<MasterDomainModel> creationInvariantPolicy()
	{
		return CreationInvariantPolicy.allowing();
	}

	default CreationExternalConsistencyPolicy<MasterDomainModel> creationExternalConsistencyPolicy()
	{
		return CreationExternalConsistencyPolicy.allowing();
	}
}