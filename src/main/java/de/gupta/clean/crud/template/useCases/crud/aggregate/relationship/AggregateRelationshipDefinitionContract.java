package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics;

import java.util.Collection;

/**
 * Consumer-facing declaration contract for a master-to-satellite relationship.
 * <p>
 * Runtime support is currently limited to:
 * <ul>
 *     <li>{@link Cardinality#ONE} with {@link ReconciliationStrategy#REPLACE}</li>
 *     <li>{@link Cardinality#MANY} with {@link ReconciliationStrategy#REPLACE}</li>
 *     <li>{@link Cardinality#MANY} with {@link ReconciliationStrategy#MERGE_BY_ID}</li>
 * </ul>
 */
public interface AggregateRelationshipDefinitionContract<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch>
{
	String name();

	Cardinality cardinality();

	LifecycleSemantics lifecycleSemantics();

	/**
	 * Resolves all create-time satellite intents from the master create input.
	 * <p>
	 * For {@link Cardinality#REPLACE} semantics on collections, the resolved intents describe the full requested
	 * linked set.
	 */
	SatelliteCreateInputResolver<MasterDomainModelCreate, ? extends Collection<? extends SatelliteCreateIntent<?, ?>>>
	createInputResolver();

	/**
	 * Resolves all update-time satellite mutation intents from the master patch input.
	 * <p>
	 * For {@link ReconciliationStrategy#MERGE_BY_ID}, update and remove intents must target explicit satellite
	 * domain IDs.
	 */
	SatellitePatchInputResolver<MasterDomainModelUpdatePatch,
			? extends Collection<? extends SatelliteMutationIntent<?, ?, ?>>> patchInputResolver();

	ReconciliationStrategy reconciliationStrategy();
}
