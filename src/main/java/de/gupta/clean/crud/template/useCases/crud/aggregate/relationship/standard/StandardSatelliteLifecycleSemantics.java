package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics;

public final class StandardSatelliteLifecycleSemantics
{
	public static LifecycleSemantics idBackedDefaults()
	{
		return LifecycleSemantics.of(true, true, false, false, true);
	}

	public static LifecycleSemantics referenceOnlyDefaults()
	{
		return LifecycleSemantics.of(false, true, false, false, true);
	}

	private StandardSatelliteLifecycleSemantics()
	{
	}
}
