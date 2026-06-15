package de.gupta.clean.crud.template.domain.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;

public enum StandardSatelliteLifecycleSemantics
{
	;

	public static LifecycleSemantics idBackedDefaults()
	{
		return LifecycleSemantics.of(true, true, false, false, true);
	}

	public static LifecycleSemantics referenceOnlyDefaults()
	{
		return LifecycleSemantics.of(false, true, false, false, true);
	}

}