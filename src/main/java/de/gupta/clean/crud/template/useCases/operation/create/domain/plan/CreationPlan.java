package de.gupta.clean.crud.template.useCases.operation.create.domain.plan;

import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.util.Objects;

public record CreationPlan<DomainModel>(
		String aggregateKey,
		DomainModel domainModel)
{
	public static <DomainModel> CreationPlan<DomainModel> of(final String aggregateKey,
	                                                         final DomainModel createModel)
	{
		return new CreationPlan<>(aggregateKey, createModel);
	}

	public CreationPlan
	{
		Objects.requireNonNull(domainModel, "domainModel");
		StringSanitizationUtility.requireNotBlank(aggregateKey, "aggregateKey may not be blank");
	}
}