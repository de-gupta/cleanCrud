package de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleSemanticsTest
{
	@Test
	void noneReturnsLifecycleSemanticsWithAllFlagsDisabled()
	{
		LifecycleSemantics lifecycleSemantics = LifecycleSemantics.none();

		assertFalse(lifecycleSemantics.cascadeCreate());
		assertFalse(lifecycleSemantics.cascadeUpdate());
		assertFalse(lifecycleSemantics.cascadeDelete());
		assertFalse(lifecycleSemantics.orphanDelete());
		assertFalse(lifecycleSemantics.hydrateOnFetch());
	}

	@Test
	void ofPreservesAllProvidedFlags()
	{
		LifecycleSemantics lifecycleSemantics = LifecycleSemantics.of(true, false, true, false, true);

		assertTrue(lifecycleSemantics.cascadeCreate());
		assertFalse(lifecycleSemantics.cascadeUpdate());
		assertTrue(lifecycleSemantics.cascadeDelete());
		assertFalse(lifecycleSemantics.orphanDelete());
		assertTrue(lifecycleSemantics.hydrateOnFetch());
	}
}
