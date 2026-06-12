package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.invariant;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.invariant.InvariantViolation;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface DomainInvariantPolicy<DomainModel>
{
	static <DomainModel> DomainInvariantPolicy<DomainModel> allowing()
	{
		return (_, _, _) -> List.of();
	}

	Collection<InvariantViolation> invariantViolationsFor(
			final OperationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}