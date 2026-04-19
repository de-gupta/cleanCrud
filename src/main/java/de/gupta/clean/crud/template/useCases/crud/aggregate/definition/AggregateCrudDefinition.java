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

import java.util.Collection;

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

	Collection<AggregateRelationshipDefinitionContract<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>> relationshipDefinitions();
}
