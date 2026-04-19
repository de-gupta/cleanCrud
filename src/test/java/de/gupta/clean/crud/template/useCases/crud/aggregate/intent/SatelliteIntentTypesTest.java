package de.gupta.clean.crud.template.useCases.crud.aggregate.intent;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatelliteIntentTypesTest
{
	@Test
	void satelliteCreateIntentIsSealedWithExpectedVariants()
	{
		Set<String> permittedTypeNames = Arrays.stream(SatelliteCreateIntent.class.getPermittedSubclasses())
		                                       .map(Class::getSimpleName)
		                                       .collect(Collectors.toSet());

		assertTrue(SatelliteCreateIntent.class.isSealed());
		assertEquals(Set.of(
				"NoSatelliteCreateIntent",
				"ReferenceSatelliteCreateIntent",
				"InlineSatelliteCreateIntent"), permittedTypeNames);
	}

	@Test
	void satelliteMutationIntentIsSealedWithExpectedVariants()
	{
		Set<String> permittedTypeNames = Arrays.stream(SatelliteMutationIntent.class.getPermittedSubclasses())
		                                       .map(Class::getSimpleName)
		                                       .collect(Collectors.toSet());

		assertTrue(SatelliteMutationIntent.class.isSealed());
		assertEquals(Set.of(
				"ReferenceSatelliteMutationIntent",
				"CreateSatelliteMutationIntent",
				"UpdateSatelliteMutationIntent",
				"RemoveSatelliteMutationIntent",
				"UpdateCurrentSatelliteMutationIntent",
				"RemoveCurrentSatelliteMutationIntent",
				"UpsertCurrentSatelliteMutationIntent"), permittedTypeNames);
	}
}