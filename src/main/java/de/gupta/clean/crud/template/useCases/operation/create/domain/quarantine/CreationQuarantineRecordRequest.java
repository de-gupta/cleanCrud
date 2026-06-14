package de.gupta.clean.crud.template.useCases.operation.create.domain.quarantine;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationViolation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record CreationQuarantineRecordRequest(
		CreationOperationContext context,
		String aggregateKey,
		CreationOperationRequest<?> request,
		Collection<CreationOperationViolation> blockingViolations,
		Collection<CreationOperationViolation> toleratedViolations)
{
	public CreationQuarantineRecordRequest
	{
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(aggregateKey, "aggregateKey");
		if (aggregateKey.isBlank())
		{
			throw new IllegalArgumentException("aggregateKey must not be blank");
		}
		Objects.requireNonNull(request, "request");
		blockingViolations = List.copyOf(Objects.requireNonNull(blockingViolations, "blockingViolations"));
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
		if (blockingViolations.isEmpty())
		{
			throw new IllegalArgumentException("blockingViolations must not be empty");
		}
	}
}
