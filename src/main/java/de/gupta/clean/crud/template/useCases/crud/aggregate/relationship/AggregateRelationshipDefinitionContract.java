package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;

import java.util.Collection;

public interface AggregateRelationshipDefinitionContract<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch>
{
	String name();

	Cardinality cardinality();

	LifecycleSemantics lifecycleSemantics();

	SatelliteCreateInputResolver<MasterDomainModelCreate, ? extends Collection<? extends SatelliteCreateIntent<?, ?>>>
	createInputResolver();

	SatellitePatchInputResolver<MasterDomainModelUpdatePatch,
			? extends Collection<? extends SatelliteMutationIntent<?, ?, ?>>> patchInputResolver();

	ReconciliationStrategy reconciliationStrategy();
}