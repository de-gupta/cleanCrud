package de.gupta.clean.crud.template.useCases.crud.aggregate.builder;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinitionContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class AggregateCrudDefinitions
{
	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel,
			MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse>
	aggregateCrudDefinition()
	{
		return new AggregateCrudDefinitionBuilder<>();
	}

	private static <Value> Value required(final Value value, final String name)
	{
		return Objects.requireNonNull(value, name);
	}

	private AggregateCrudDefinitions()
	{
	}

	public static final class AggregateCrudDefinitionBuilder<
			MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	{
		private final List<AggregateRelationshipDefinitionContract<MasterDomainId, MasterDomainModel,
				MasterDomainModelCreate, MasterDomainModelUpdatePatch>> relationshipDefinitions = new ArrayList<>();
		private AggregateMutationPort<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch> mutationPort;
		private AggregateFetchPort<MasterDomainId, MasterDomainModel> fetchPort;
		private DomainModelBuilder<MasterDomainModelCreate, MasterDomainModel> createBuilder;
		private DomainModelPatcher<MasterDomainModel, MasterDomainModelUpdatePatch> patcher;
		private DomainResponseBuilder<MasterDomainModel, MasterDomainModelResponse> responseBuilder;
		private InsertionPolicy<MasterDomainModel> insertionPolicy;
		private PatchPolicy<MasterDomainModel> patchPolicy;
		private DeletionPolicy<MasterDomainModel> deletionPolicy;
		private DomainSecurityPolicy<MasterDomainModel> securityPolicy;
		private DuplicateDefinition<MasterDomainModel> duplicateDefinition;

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> mutationPort(
				final AggregateMutationPort<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch> mutationPort)
		{
			this.mutationPort = mutationPort;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> fetchPort(
				final AggregateFetchPort<MasterDomainId, MasterDomainModel> fetchPort)
		{
			this.fetchPort = fetchPort;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> createBuilder(
				final DomainModelBuilder<MasterDomainModelCreate, MasterDomainModel> createBuilder)
		{
			this.createBuilder = createBuilder;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> patcher(
				final DomainModelPatcher<MasterDomainModel, MasterDomainModelUpdatePatch> patcher)
		{
			this.patcher = patcher;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> responseBuilder(
				final DomainResponseBuilder<MasterDomainModel, MasterDomainModelResponse> responseBuilder)
		{
			this.responseBuilder = responseBuilder;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> insertionPolicy(
				final InsertionPolicy<MasterDomainModel> insertionPolicy)
		{
			this.insertionPolicy = insertionPolicy;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> patchPolicy(
				final PatchPolicy<MasterDomainModel> patchPolicy)
		{
			this.patchPolicy = patchPolicy;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> deletionPolicy(
				final DeletionPolicy<MasterDomainModel> deletionPolicy)
		{
			this.deletionPolicy = deletionPolicy;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> securityPolicy(
				final DomainSecurityPolicy<MasterDomainModel> securityPolicy)
		{
			this.securityPolicy = securityPolicy;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> duplicateDefinition(
				final DuplicateDefinition<MasterDomainModel> duplicateDefinition)
		{
			this.duplicateDefinition = duplicateDefinition;
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> relationshipDefinition(
				final AggregateRelationshipDefinitionContract<MasterDomainId, MasterDomainModel,
						MasterDomainModelCreate, MasterDomainModelUpdatePatch> relationshipDefinition)
		{
			this.relationshipDefinitions.add(relationshipDefinition);
			return this;
		}

		public AggregateCrudDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> relationshipDefinitions(
				final Collection<? extends AggregateRelationshipDefinitionContract<MasterDomainId, MasterDomainModel,
						MasterDomainModelCreate, MasterDomainModelUpdatePatch>> relationshipDefinitions)
		{
			this.relationshipDefinitions.clear();
			this.relationshipDefinitions.addAll(relationshipDefinitions);
			return this;
		}

		public AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, MasterDomainModelResponse> build()
		{
			return new BuiltAggregateCrudDefinition<>(
					required(mutationPort, "mutationPort"),
					required(fetchPort, "fetchPort"),
					required(createBuilder, "createBuilder"),
					required(patcher, "patcher"),
					required(responseBuilder, "responseBuilder"),
					required(insertionPolicy, "insertionPolicy"),
					required(patchPolicy, "patchPolicy"),
					required(deletionPolicy, "deletionPolicy"),
					required(securityPolicy, "securityPolicy"),
					required(duplicateDefinition, "duplicateDefinition"),
					List.copyOf(relationshipDefinitions));
		}

		private AggregateCrudDefinitionBuilder()
		{
		}
	}

	private record BuiltAggregateCrudDefinition<
			MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>(
			AggregateMutationPort<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch> mutationPort,
			AggregateFetchPort<MasterDomainId, MasterDomainModel> fetchPort,
			DomainModelBuilder<MasterDomainModelCreate, MasterDomainModel> createBuilder,
			DomainModelPatcher<MasterDomainModel, MasterDomainModelUpdatePatch> patcher,
			DomainResponseBuilder<MasterDomainModel, MasterDomainModelResponse> responseBuilder,
			InsertionPolicy<MasterDomainModel> insertionPolicy,
			PatchPolicy<MasterDomainModel> patchPolicy,
			DeletionPolicy<MasterDomainModel> deletionPolicy,
			DomainSecurityPolicy<MasterDomainModel> securityPolicy,
			DuplicateDefinition<MasterDomainModel> duplicateDefinition,
			Collection<AggregateRelationshipDefinitionContract<MasterDomainId, MasterDomainModel,
					MasterDomainModelCreate, MasterDomainModelUpdatePatch>> relationshipDefinitions)
			implements AggregateCrudDefinition<MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			MasterDomainModelResponse>
	{
	}
}