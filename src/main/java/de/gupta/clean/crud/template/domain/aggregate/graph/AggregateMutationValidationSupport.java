package de.gupta.clean.crud.template.domain.aggregate.graph;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.equality.KeyBasedDuplicateDefinition;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.aggregate.policy.AggregateMutationPolicies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public final class AggregateMutationValidationSupport
{
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateSaveModels(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final Collection<MasterDomainModel> domainModels)
	{
		if (!domainModels.stream().allMatch(definition.securityPolicy()::isAccessAllowed))
		{
			throw AccessDeniedException.withMessage("Access not allowed for one or more models");
		}
		throwIfDuplicatesInCollection(domainModels, definition.duplicateDefinition());
		domainModels.forEach(definition.insertionPolicy()::validateInsertion);
	}

	private <MasterDomainModel> void throwIfDuplicatesInCollection(
			final Collection<MasterDomainModel> models,
			final DuplicateDefinition<MasterDomainModel> duplicateDefinition)
	{
		if (duplicateDefinition instanceof KeyBasedDuplicateDefinition<MasterDomainModel, ?> keyBasedDuplicateDefinition)
		{
			throwIfDuplicateKeysInCollection(models, keyBasedDuplicateDefinition);
			return;
		}
		throwIfDuplicateDefinitionsInCollection(models, duplicateDefinition);
	}

	private <MasterDomainModel> void throwIfDuplicateKeysInCollection(
			final Collection<MasterDomainModel> models,
			final KeyBasedDuplicateDefinition<MasterDomainModel, ?> keyBasedDuplicateDefinition)
	{
		var seenKeys = new HashSet<>();
		for (var model : models)
		{
			if (!seenKeys.add(keyBasedDuplicateDefinition.duplicateKeyOf(model)))
			{
				throw InvalidRequestException.withMessage("The collection contains duplicate elements");
			}
		}
	}

	private <MasterDomainModel> void throwIfDuplicateDefinitionsInCollection(
			final Collection<MasterDomainModel> models,
			final DuplicateDefinition<MasterDomainModel> duplicateDefinition)
	{
		var seen = new ArrayList<MasterDomainModel>();
		for (var model : models)
		{
			if (seen.stream().anyMatch(existing -> duplicateDefinition.areDuplicates(existing, model)))
			{
				throw InvalidRequestException.withMessage("The collection contains duplicate elements");
			}
			seen.add(model);
		}
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateAccessAndValidatePatch(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel original,
			final MasterDomainModel replacement)
	{
		validateSourceAwarePatch(definition, OperationSource.USER_INTENT, original, replacement);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateSourceAwarePatch(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final OperationSource source,
			final MasterDomainModel original,
			final MasterDomainModel replacement)
	{
		AggregateMutationPolicies.sourceAwarePolicy(definition).validate(source, original, replacement);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateDeletion(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel model)
	{
		validateAccess(definition, model);
		definition.deletionPolicy().validateDeletion(model);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateAccess(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel model)
	{
		if (!definition.securityPolicy().isAccessAllowed(model))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
	}
}