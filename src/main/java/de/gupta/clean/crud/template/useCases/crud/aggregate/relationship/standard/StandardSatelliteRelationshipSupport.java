package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatelliteHydrationStrategy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatelliteLinkStrategy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatellitePersistenceOrder;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

final class StandardSatelliteRelationshipSupport
{
	static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel, SatelliteAggregateResponse, SatellitePublicResponse>
	SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> oneToOneLinkStrategy(
			final String relationshipName,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, SatelliteAggregateResponse>
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
				                       .map(StandardSatelliteRelationshipSupport::requiredId);
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
						                  .map(satelliteDomainId -> fetchPublicResponse(
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
						          .map(satellite -> toPublicResponse(
										  satelliteDefinition,
										  publicResponseMapper,
										  satellite)));
			}
		};
	}

	static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel, SatelliteAggregateResponse, SatellitePublicResponse>
	SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> oneToManyLinkStrategy(
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, SatelliteAggregateResponse>
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
				                        .<SatelliteDomainId>map(StandardSatelliteRelationshipSupport::requiredId)
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
						                  .map(satelliteDomainId -> fetchPublicResponse(
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
						          .map(satellite -> toPublicResponse(
										  satelliteDefinition,
										  publicResponseMapper,
										  satellite))
						          .toList());
			}
		};
	}

	static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	defaultHydrationStrategy()
	{
		return (master, satelliteFetchPort, satelliteLinkStrategy) ->
		{
			var hydratedSatellites = new ArrayList<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>();
			for (var satelliteDomainId : satelliteLinkStrategy.currentLinkedSatelliteDomainIds(master.model()))
			{
				satelliteFetchPort.findById(satelliteDomainId).ifPresent(hydratedSatellites::add);
			}
			return satelliteLinkStrategy.attachHydratedSatellites(master.model(), hydratedSatellites);
		};
	}

	static <SatelliteDomainId, SatelliteAggregateResponse, SatellitePublicResponse>
	SatellitePublicResponse mapPublicResponse(
			final Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
					publicResponseMapper,
			final SatelliteDomainId satelliteDomainId,
			final SatelliteAggregateResponse satelliteAggregateResponse)
	{
		return publicResponseMapper.apply(IdentifiedModel.of(satelliteDomainId, satelliteAggregateResponse));
	}

	@SuppressWarnings("unchecked")
	static <SatelliteDomainId, SatellitePublicResponse> SatelliteDomainId requiredId(
			final SatellitePublicResponse satellitePublicResponse)
	{
		try
		{
			var idMethod = satellitePublicResponse.getClass().getDeclaredMethod("id");
			idMethod.setAccessible(true);
			return (SatelliteDomainId) idMethod.invoke(
					satellitePublicResponse);
		}
		catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
		{
			throw InvalidRequestException.withMessage(
					"Standard satellite relationships require public response type '%s' to expose an id() accessor".formatted(
							satellitePublicResponse.getClass().getName()));
		}
	}

	private static <SatelliteDomainId, SatelliteDomainModel, SatelliteAggregateResponse, SatellitePublicResponse>
	SatellitePublicResponse fetchPublicResponse(
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, SatelliteAggregateResponse>
					satelliteDefinition,
			final Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
					publicResponseMapper,
			final SatelliteDomainId satelliteDomainId)
	{
		var satellite = satelliteDefinition.fetchPort()
		                                   .findById(satelliteDomainId)
		                                   .orElseThrow(
												   () -> ResourceNotFoundException.withId(satelliteDomainId));
		return toPublicResponse(satelliteDefinition, publicResponseMapper, satellite);
	}

	private static <SatelliteDomainId, SatelliteDomainModel, SatelliteAggregateResponse, SatellitePublicResponse>
	SatellitePublicResponse toPublicResponse(
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, SatelliteAggregateResponse>
					satelliteDefinition,
			final Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
					publicResponseMapper,
			final IdentifiedModel<SatelliteDomainId, SatelliteDomainModel> satellite)
	{
		return publicResponseMapper.apply(
				IdentifiedModel.of(
						satellite.id(),
						satelliteDefinition.responseBuilder().toResponse(satellite.model())));
	}

	private StandardSatelliteRelationshipSupport()
	{
	}
}