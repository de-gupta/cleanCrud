package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.plan;

import java.util.Objects;
import java.util.Optional;

public record AggregateCreationPlan<DomainModelCreate>(
		Optional<DomainModelCreate> rootCreate)
{
	public static <DomainModelCreate> AggregateCreationPlan<DomainModelCreate> rootOnly(
			final DomainModelCreate rootCreate)
	{
		return new AggregateCreationPlan<>(Optional.of(rootCreate));
	}

	public AggregateCreationPlan
	{
		Objects.requireNonNull(rootCreate, "rootCreate");
	}
}