package de.gupta.clean.crud.template.domain.aggregate.graph;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinitionContract;
import de.gupta.clean.crud.template.domain.aggregate.relationship.Cardinality;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// private or package private constructor?
public final class AggregateDefinitionRelationshipInspector
{
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?>> satelliteRelationships(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition)
	{
		Collection<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?>>
				relationships = new ArrayList<>();
		for (var contract : definition.relationshipDefinitions())
		{
			var relationship = typedRelationship(contract);
			validateRelationship(relationship);
			relationships.add(relationship);
		}
		return List.copyOf(relationships);
	}

	@SuppressWarnings("unchecked")
	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?> typedRelationship(
			final AggregateRelationshipDefinitionContract<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> contract)
	{
		if (!(contract instanceof AggregateRelationshipDefinition<?, ?, ?, ?, ?, ?, ?, ?> relationshipDefinition))
		{
			throw AggregateRelationshipExecutionNotSupportedException.withMessage(
					"Only AggregateRelationshipDefinition instances are executable at runtime");
		}
		return (AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?>) relationshipDefinition;
	}

	private void validateRelationship(final AggregateRelationshipDefinitionContract<?, ?, ?, ?> relationship)
	{
		if (relationship.cardinality() == Cardinality.ONE && relationship.reconciliationStrategy() == ReconciliationStrategy.MERGE_BY_ID)
		{
			throw AggregateRelationshipExecutionNotSupportedException.withMessage(
					"MERGE_BY_ID is only supported for MANY satellite relationships");
		}
		if (relationship.cardinality() == Cardinality.MANY && relationship.reconciliationStrategy() != ReconciliationStrategy.REPLACE && relationship.reconciliationStrategy() != ReconciliationStrategy.MERGE_BY_ID)
		{
			throw AggregateRelationshipExecutionNotSupportedException.withMessage(
					"Only REPLACE and MERGE_BY_ID are supported for MANY satellite relationships");
		}
	}

	AggregateDefinitionRelationshipInspector()
	{
	}
}