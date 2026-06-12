package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.invariant;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.invariant.InvariantViolation;

import java.util.List;

@FunctionalInterface
public interface CreationInvariantPolicy<DomainModel>
{
	static <DomainModel> CreationInvariantPolicy<DomainModel> allowing()
	{
		return (_, _) -> List.of();
	}

	List<InvariantViolation> invariantViolationsFor(
			final OperationSource source,
			final DomainModel afterModel);
}