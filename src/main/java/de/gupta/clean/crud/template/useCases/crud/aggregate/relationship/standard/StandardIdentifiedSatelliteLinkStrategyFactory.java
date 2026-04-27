package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatelliteLinkStrategy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatellitePersistenceOrder;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

final class StandardIdentifiedSatelliteLinkStrategyFactory
{
	static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> oneToOneLinkStrategy(
			final String relationshipName,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, ?> satelliteDefinition,
			final Function<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
					currentSatellite,
			final BiFunction<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>,
					MasterDomainModel> replaceSatellite)
	{
		return new SatelliteLinkStrategy<>()
		{
			@Override
			public SatellitePersistenceOrder persistenceOrder()
			{
				return SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER;
			}

			@Override
			public Optional<SatelliteDomainId> currentLinkedSatelliteDomainId(final MasterDomainModel masterDomainModel)
			{
				return currentSatellite.apply(masterDomainModel).map(IdentifiedModel::id);
			}

			@Override
			public Collection<SatelliteDomainId> currentLinkedSatelliteDomainIds(
					final MasterDomainModel masterDomainModel)
			{
				return currentLinkedSatelliteDomainId(masterDomainModel).stream().toList();
			}

			@Override
			public MasterDomainModel replaceLinkedSatelliteDomainIds(
					final MasterDomainModel masterDomainModel,
					final Collection<SatelliteDomainId> satelliteDomainIds)
			{
				if (satelliteDomainIds.size() > 1)
				{
					throw InvalidRequestException.withMessage(
							"Relationship '%s' cannot relink more than one satellite for ONE cardinality".formatted(
									relationshipName));
				}
				return replaceSatellite.apply(
						masterDomainModel,
						satelliteDomainIds.stream()
						                  .findFirst()
						                  .map(satelliteDomainId -> satelliteDefinition.fetchPort()
						                                                               .findById(satelliteDomainId)
						                                                               .orElseThrow()));
			}

			@Override
			public MasterDomainModel attachHydratedSatellites(
					final MasterDomainModel masterDomainModel,
					final Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>> satellites)
			{
				if (satellites.size() > 1)
				{
					throw InvalidRequestException.withMessage(
							"Relationship '%s' cannot hydrate more than one satellite for ONE cardinality".formatted(
									relationshipName));
				}
				return replaceSatellite.apply(masterDomainModel, satellites.stream().findFirst());
			}
		};
	}

	static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> oneToManyLinkStrategy(
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, ?> satelliteDefinition,
			final Function<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
					currentSatellites,
			final BiFunction<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>,
					MasterDomainModel> replaceSatellites)
	{
		return new SatelliteLinkStrategy<>()
		{
			@Override
			public SatellitePersistenceOrder persistenceOrder()
			{
				return SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER;
			}

			@Override
			public Optional<SatelliteDomainId> currentLinkedSatelliteDomainId(final MasterDomainModel masterDomainModel)
			{
				return currentLinkedSatelliteDomainIds(masterDomainModel).stream().findFirst();
			}

			@Override
			public Collection<SatelliteDomainId> currentLinkedSatelliteDomainIds(
					final MasterDomainModel masterDomainModel)
			{
				return currentSatellites.apply(masterDomainModel).stream().map(IdentifiedModel::id).toList();
			}

			@Override
			public MasterDomainModel replaceLinkedSatelliteDomainIds(
					final MasterDomainModel masterDomainModel,
					final Collection<SatelliteDomainId> satelliteDomainIds)
			{
				return replaceSatellites.apply(
						masterDomainModel,
						satelliteDomainIds.stream()
						                  .map(satelliteDomainId -> satelliteDefinition.fetchPort()
						                                                               .findById(satelliteDomainId)
						                                                               .orElseThrow())
						                  .toList());
			}

			@Override
			public MasterDomainModel attachHydratedSatellites(
					final MasterDomainModel masterDomainModel,
					final Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>> satellites)
			{
				return replaceSatellites.apply(masterDomainModel, satellites);
			}
		};
	}

	private StandardIdentifiedSatelliteLinkStrategyFactory()
	{
	}
}