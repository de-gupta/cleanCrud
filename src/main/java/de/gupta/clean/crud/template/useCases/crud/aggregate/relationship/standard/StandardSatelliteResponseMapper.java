package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

final class StandardSatelliteResponseMapper
{
	static <SatelliteDomainId, SatelliteAggregateResponse, SatellitePublicResponse> SatellitePublicResponse mapPublicResponse(
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
			return (SatelliteDomainId) idMethod.invoke(satellitePublicResponse);
		}
		catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
		{
			throw InvalidRequestException.withMessage(
					"Standard satellite relationships require public response type '%s' to expose an id() accessor".formatted(
							satellitePublicResponse.getClass().getName()));
		}
	}

	static <SatelliteDomainId, SatelliteDomainModel, SatelliteAggregateResponse, SatellitePublicResponse>
	SatellitePublicResponse fetchPublicResponse(
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, ?, ?, SatelliteAggregateResponse>
					satelliteDefinition,
			final Function<IdentifiedModel<SatelliteDomainId, SatelliteAggregateResponse>, SatellitePublicResponse>
					publicResponseMapper,
			final SatelliteDomainId satelliteDomainId)
	{
		var satellite = satelliteDefinition.fetchPort()
		                                   .findById(satelliteDomainId)
		                                   .orElseThrow(() -> ResourceNotFoundException.withId(satelliteDomainId));
		return toPublicResponse(satelliteDefinition, publicResponseMapper, satellite);
	}

	static <SatelliteDomainId, SatelliteDomainModel, SatelliteAggregateResponse, SatellitePublicResponse>
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

	private StandardSatelliteResponseMapper()
	{
	}
}
