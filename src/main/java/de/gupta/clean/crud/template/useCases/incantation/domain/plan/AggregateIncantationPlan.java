package de.gupta.clean.crud.template.useCases.incantation.domain.plan;

import java.util.Objects;
import java.util.Optional;

public record AggregateIncantationPlan<DomainModelCreate>(
		Optional<DomainModelCreate> rootCreate)
{
	public static <DomainModelCreate> AggregateIncantationPlan<DomainModelCreate> rootOnly(
			final DomainModelCreate rootCreate)
	{
		return new AggregateIncantationPlan<>(Optional.of(rootCreate));
	}

	public AggregateIncantationPlan
	{
		Objects.requireNonNull(rootCreate, "rootCreate");
	}
}
