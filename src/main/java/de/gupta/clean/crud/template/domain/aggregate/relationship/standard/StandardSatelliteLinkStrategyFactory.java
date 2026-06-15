package de.gupta.clean.crud.template.domain.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.SatelliteLinkStrategy;
import de.gupta.clean.crud.template.domain.aggregate.relationship.SatellitePersistenceOrder;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

enum StandardSatelliteLinkStrategyFactory
{
	;

	static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel, SatelliteAggregateResponse, SatellitePublicResponse>
	SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> oneToOneLinkStrategy(
			final String relationshipName,
			final AggregateDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, SatelliteAggregateResponse>
					satelliteDefinition,
			final Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
					publicResponseMapper,
			final Function<MasterDomainModel, Optional<SatellitePublicResponse>> currentSatellite,
			final BiFunction<MasterDomainModel, Optional<SatellitePublicResponse>, MasterDomainModel> replaceSatellite)
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
				return currentSatellite.apply(masterDomainModel)
				                       .map(StandardSatelliteResponseMapper::requiredId);
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
						                  .map(satelliteDomainId -> StandardSatelliteResponseMapper.fetchPublicResponse(
												  satelliteDefinition,
												  publicResponseMapper,
												  satelliteDomainId)));
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
				return replaceSatellite.apply(
						masterDomainModel,
						satellites.stream()
						          .findFirst()
						          .map(satellite -> StandardSatelliteResponseMapper.toPublicResponse(
										  satelliteDefinition,
										  publicResponseMapper,
										  satellite)));
			}
		};
	}

	static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel, SatelliteAggregateResponse, SatellitePublicResponse>
	SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> oneToManyLinkStrategy(
			final AggregateDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, SatelliteAggregateResponse>
					satelliteDefinition,
			final Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
					publicResponseMapper,
			final Function<MasterDomainModel, Collection<SatellitePublicResponse>> currentSatellites,
			final BiFunction<MasterDomainModel, Collection<SatellitePublicResponse>, MasterDomainModel>
					replaceSatellites)
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
				return currentSatellites.apply(masterDomainModel)
				                        .stream()
				                        .<SatelliteDomainId>map(StandardSatelliteResponseMapper::requiredId)
				                        .toList();
			}

			@Override
			public MasterDomainModel replaceLinkedSatelliteDomainIds(
					final MasterDomainModel masterDomainModel,
					final Collection<SatelliteDomainId> satelliteDomainIds)
			{
				return replaceSatellites.apply(
						masterDomainModel,
						satelliteDomainIds.stream()
						                  .map(satelliteDomainId -> StandardSatelliteResponseMapper.fetchPublicResponse(
												  satelliteDefinition,
												  publicResponseMapper,
												  satelliteDomainId))
						                  .toList());
			}

			@Override
			public MasterDomainModel attachHydratedSatellites(
					final MasterDomainModel masterDomainModel,
					final Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>> satellites)
			{
				return replaceSatellites.apply(
						masterDomainModel,
						satellites.stream()
						          .map(satellite -> StandardSatelliteResponseMapper.toPublicResponse(
										  satelliteDefinition,
										  publicResponseMapper,
										  satellite))
						          .toList());
			}
		};
	}

}