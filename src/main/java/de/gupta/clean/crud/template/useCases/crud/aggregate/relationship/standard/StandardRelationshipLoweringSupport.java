package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.Relationship;
import de.gupta.clean.crud.template.domain.relationship.RelationshipKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.builder.AggregateRelationshipDefinitions;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

final class StandardRelationshipLoweringSupport
{
	static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch> buildOne(
			final Relationship relationship,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, ?> satelliteDefinition,
			final Function<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
					current,
			final BiFunction<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>,
					MasterDomainModel> replace)
	{
		return AggregateRelationshipDefinitions
				.<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
						SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch>aggregateRelationshipDefinition()
				.name(relationship.propertyName())
				.cardinality(Cardinality.ONE)
				.relationshipKind(relationship.relationshipKind())
				.lifecycleSemantics(relationship.lifecycleSemantics())
				.satelliteDefinition(satelliteDefinition)
				.createInputResolver(oneCreateInputResolver(relationship))
				.patchInputResolver(onePatchInputResolver(relationship))
				.identityResolver(oneIdentityResolver(current))
				.reconciliationStrategy(effectiveReconciliationStrategy(relationship, Cardinality.ONE))
				.linkStrategy(StandardIdentifiedSatelliteLinkStrategyFactory.oneToOneLinkStrategy(
						relationship.propertyName(),
						satelliteDefinition,
						current,
						replace))
				.hydrationStrategy(relationship.lifecycleSemantics().hydrateOnFetch()
						? StandardSatelliteHydrationStrategyFactory.defaultHydrationStrategy()
						: null)
				.build();
	}

	static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch> buildMany(
			final Relationship relationship,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, ?> satelliteDefinition,
			final Function<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
					current,
			final BiFunction<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>,
					MasterDomainModel> replace)
	{
		return AggregateRelationshipDefinitions
				.<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
						SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch>aggregateRelationshipDefinition()
				.name(relationship.propertyName())
				.cardinality(Cardinality.MANY)
				.relationshipKind(relationship.relationshipKind())
				.lifecycleSemantics(relationship.lifecycleSemantics())
				.satelliteDefinition(satelliteDefinition)
				.createInputResolver(manyCreateInputResolver(relationship))
				.patchInputResolver(manyPatchInputResolver(relationship))
				.identityResolver(manyIdentityResolver(current))
				.reconciliationStrategy(effectiveReconciliationStrategy(relationship, Cardinality.MANY))
				.linkStrategy(StandardIdentifiedSatelliteLinkStrategyFactory.oneToManyLinkStrategy(
						satelliteDefinition,
						current,
						replace))
				.hydrationStrategy(relationship.lifecycleSemantics().hydrateOnFetch()
						? StandardSatelliteHydrationStrategyFactory.defaultHydrationStrategy()
						: null)
				.build();
	}

	private static <MasterDomainModelCreate, SatelliteDomainId, SatelliteDomainModelCreate>
	SatelliteCreateInputResolver<MasterDomainModelCreate, Collection<SatelliteCreateIntent<SatelliteDomainId,
			SatelliteDomainModelCreate>>> oneCreateInputResolver(final Relationship relationship)
	{
		return masterCreate ->
		{
			var propertyValue = propertyValue(masterCreate, relationship.propertyName());
			if (relationship.relationshipKind() == RelationshipKind.REFERENCED)
			{
				return optionalValue(propertyValue).stream()
				                                   .<SatelliteCreateIntent<SatelliteDomainId,
														   SatelliteDomainModelCreate>>map(
														   satelliteDomainId ->
																   new SatelliteCreateIntent.ReferenceSatelliteCreateIntent<>(
																		   cast(satelliteDomainId)))
				                                   .toList();
			}
			return optionalValue(propertyValue).stream()
			                                   .<SatelliteCreateIntent<SatelliteDomainId,
													   SatelliteDomainModelCreate>>map(
													   satelliteDomainModelCreate ->
															   new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
																	   cast(satelliteDomainModelCreate)))
			                                   .toList();
		};
	}

	private static <MasterDomainModelCreate, SatelliteDomainId, SatelliteDomainModelCreate>
	SatelliteCreateInputResolver<MasterDomainModelCreate, Collection<SatelliteCreateIntent<SatelliteDomainId,
			SatelliteDomainModelCreate>>> manyCreateInputResolver(final Relationship relationship)
	{
		return masterCreate ->
		{
			var propertyValue = propertyValue(masterCreate, relationship.propertyName());
			if (relationship.relationshipKind() == RelationshipKind.REFERENCED)
			{
				return collectionValue(propertyValue).stream()
				                                     .<SatelliteCreateIntent<SatelliteDomainId,
															 SatelliteDomainModelCreate>>map(
															 satelliteDomainId ->
																	 new SatelliteCreateIntent.ReferenceSatelliteCreateIntent<>(
																			 cast(satelliteDomainId)))
				                                     .toList();
			}
			return collectionValue(propertyValue).stream()
			                                     .<SatelliteCreateIntent<SatelliteDomainId,
														 SatelliteDomainModelCreate>>map(
														 satelliteDomainModelCreate ->
																 new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
																		 cast(satelliteDomainModelCreate)))
			                                     .toList();
		};
	}

	private static <MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	SatellitePatchInputResolver<MasterDomainModelUpdatePatch, Collection<SatelliteMutationIntent<SatelliteDomainId,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>> onePatchInputResolver(
			final Relationship relationship)
	{
		return masterPatch ->
		{
			var mutationIntents = new ArrayList<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch>>();
			var propertyValue = propertyValue(masterPatch, relationship.propertyName());
			var removeIds = List.copyOf(removeIds(masterPatch, relationship));
			if (optionalValue(propertyValue).isPresent() && !removeIds.isEmpty())
			{
				throw InvalidRequestException.withMessage(
						"Relationship '%s' cannot combine a nested patch item with remove ids for ONE cardinality".formatted(
								relationship.propertyName()));
			}
			if (removeIds.size() > 1)
			{
				throw InvalidRequestException.withMessage(
						"Relationship '%s' cannot remove more than one satellite for ONE cardinality".formatted(
								relationship.propertyName()));
			}
			if (relationship.relationshipKind() == RelationshipKind.REFERENCED)
			{
				optionalValue(propertyValue).ifPresent(satelliteDomainId -> mutationIntents.add(
						new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(cast(satelliteDomainId))));
			}
			else
			{
				optionalValue(propertyValue).ifPresent(satelliteDomainModelUpdatePatch ->
						mutationIntents.add(new SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<>(
								createFromUpdatePatch(cast(satelliteDomainModelUpdatePatch)),
								cast(satelliteDomainModelUpdatePatch))));
			}
			removeIds.forEach(satelliteDomainId -> mutationIntents.add(
					new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(cast(satelliteDomainId))));
			return mutationIntents;
		};
	}

	private static <MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	SatellitePatchInputResolver<MasterDomainModelUpdatePatch, Collection<SatelliteMutationIntent<SatelliteDomainId,
			SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>> manyPatchInputResolver(
			final Relationship relationship)
	{
		return masterPatch ->
		{
			var mutationIntents = new ArrayList<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch>>();
			var propertyValue = propertyValue(masterPatch, relationship.propertyName());
			if (relationship.relationshipKind() == RelationshipKind.REFERENCED)
			{
				collectionValueFromPatch(propertyValue).forEach(satelliteDomainId -> mutationIntents.add(
						new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(cast(satelliteDomainId))));
			}
			else
			{
				collectionValueFromPatch(propertyValue).forEach(item ->
				{
					var patchItem = cast(item);
					if (patchItem instanceof SatelliteUpdatePatchItem<?, ?>(Optional<?> id, Object patch) && id
							.isPresent())
					{
						mutationIntents.add(new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(
								cast(id.orElseThrow()),
								cast(patch)));
						return;
					}
					if (patchItem instanceof SatelliteUpdatePatchItem<?, ?> typedPatchItem)
					{
						mutationIntents.add(new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
								createFromUpdatePatch(cast(typedPatchItem.patch()))));
						return;
					}
					throw new IllegalArgumentException(
							"Relationship '%s' expected SatelliteUpdatePatchItem values".formatted(
									relationship.propertyName()));
				});
			}
			removeIds(masterPatch, relationship).forEach(satelliteDomainId -> mutationIntents.add(
					new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(cast(satelliteDomainId))));
			return mutationIntents;
		};
	}

	private static <MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId> oneIdentityResolver(
			final Function<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>> current)
	{
		return (masterDomainModel, satelliteDomainModel) -> current.apply(masterDomainModel)
		                                                           .filter(candidate ->
																		   Objects.equals(candidate.model(),
																				   satelliteDomainModel))
		                                                           .map(IdentifiedModel::id);
	}

	private static <MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId> manyIdentityResolver(
			final Function<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
					current)
	{
		return (masterDomainModel, satelliteDomainModel) -> current.apply(masterDomainModel).stream()
		                                                           .filter(candidate ->
																		   Objects.equals(candidate.model(),
																				   satelliteDomainModel))
		                                                           .map(IdentifiedModel::id)
		                                                           .findFirst();
	}

	private static ReconciliationStrategy effectiveReconciliationStrategy(
			final Relationship relationship,
			final Cardinality cardinality)
	{
		if (relationship.reconciliationStrategy() != null)
		{
			return relationship.reconciliationStrategy();
		}
		return cardinality == Cardinality.ONE ? ReconciliationStrategy.REPLACE : ReconciliationStrategy.MERGE_BY_ID;
	}

	private static Object propertyValue(final Object aggregatePart, final String propertyName)
	{
		try
		{
			var accessor = aggregatePart.getClass().getDeclaredMethod(propertyName);
			accessor.setAccessible(true);
			return accessor.invoke(aggregatePart);
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
		{
			throw new IllegalArgumentException(
					"Unable to resolve property accessor '%s()' on %s".formatted(
							propertyName,
							aggregatePart.getClass().getName()),
					e);
		}
	}

	private static Collection<?> removeIds(final Object patch, final Relationship relationship)
	{
		var methodName = removeFieldName(relationship.propertyName());
		try
		{
			var accessor = patch.getClass().getDeclaredMethod(methodName);
			accessor.setAccessible(true);
			return collectionValue(accessor.invoke(patch));
		}
		catch (NoSuchMethodException ignored)
		{
			return List.of();
		}
		catch (IllegalAccessException | InvocationTargetException e)
		{
			throw new IllegalArgumentException(
					"Unable to resolve remove accessor '%s()' on %s".formatted(methodName, patch.getClass().getName()),
					e);
		}
	}

	private static String removeFieldName(final String propertyName)
	{
		var singular = propertyName.endsWith("s") && propertyName.length() > 1
				? propertyName.substring(0, propertyName.length() - 1)
				: propertyName;
		return "remove" + Character.toUpperCase(singular.charAt(0)) + singular.substring(1) + "Ids";
	}

	private static Optional<?> optionalValue(final Object propertyValue)
	{
		if (propertyValue == null)
		{
			return Optional.empty();
		}
		if (propertyValue instanceof Optional<?> optional)
		{
			return optional;
		}
		return Optional.of(propertyValue);
	}

	private static Collection<?> collectionValue(final Object propertyValue)
	{
		return switch (propertyValue)
		{
			case null -> List.of();
			case Optional<?> optional ->
					optional.map(StandardRelationshipLoweringSupport::collectionValue).orElse(List.of());
			case Collection<?> collection -> collection;
			default -> List.of(propertyValue);
		};
	}

	private static Collection<?> collectionValueFromPatch(final Object propertyValue)
	{
		if (propertyValue == null)
		{
			return List.of();
		}
		if (propertyValue instanceof Optional<?> optional)
		{
			return optional.map(StandardRelationshipLoweringSupport::collectionValue).orElse(List.of());
		}
		return collectionValue(propertyValue);
	}

	@SuppressWarnings("unchecked")
	private static <Value> Value cast(final Object value)
	{
		return (Value) value;
	}

	@SuppressWarnings("unchecked")
	private static <SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch> SatelliteDomainModelCreate createFromUpdatePatch(
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch)
	{
		try
		{
			var patchClass = satelliteDomainModelUpdatePatch.getClass();
			var patchClassName = patchClass.getName();
			var createClassName = patchClassName.contains("UpdatePatch")
					? patchClassName.replace("UpdatePatch", "Create")
					: patchClassName.endsWith("Patch")
					  ? patchClassName.substring(0, patchClassName.length() - "Patch".length()) + "Create"
					  : patchClassName + "Create";
			var createClass = Class.forName(createClassName);
			var factoryMethod = createClass.getDeclaredMethod("fromUpdatePatch", patchClass);
			factoryMethod.setAccessible(true);
			return (SatelliteDomainModelCreate) factoryMethod.invoke(null, satelliteDomainModelUpdatePatch);
		}
		catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
		{
			throw new IllegalArgumentException(
					"Unable to derive create model from update patch type %s using the fromUpdatePatch convention".formatted(
							satelliteDomainModelUpdatePatch.getClass().getName()),
					e);
		}
	}

	private StandardRelationshipLoweringSupport()
	{
	}
}
