package de.gupta.clean.crud.template.useCases.operation.create.domain.plan;

import java.util.Objects;

public record CreationPlan<DomainModel>(
		String aggregateKey,
		DomainModel domainModel)
{
	public static <DomainCreateModel> CreationPlan<DomainCreateModel> of(
			final String aggregateKey,
			final DomainCreateModel createModel)
	{
		return new CreationPlan<>(aggregateKey, createModel);
	}

	public CreationPlan
	{
		Objects.requireNonNull(aggregateKey, "aggregateKey");
		Objects.requireNonNull(domainModel, "domainModel");
		if (aggregateKey.isBlank())
		{
			throw new IllegalArgumentException("aggregateKey may not be blank");
		}
	}
}