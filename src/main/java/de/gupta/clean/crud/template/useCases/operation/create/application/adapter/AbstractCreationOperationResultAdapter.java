package de.gupta.clean.crud.template.useCases.operation.create.application.adapter;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.*;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.*;

import java.util.Collection;
import java.util.List;

public abstract class AbstractCreationOperationResultAdapter<DomainModel, APIModel>
		implements CreationOperationResultAdapter<DomainModel, APIModel>
{
	@Override
	public CreateApplicationResult<APIModel> mapToAPIResult(final CreateOperationResult<DomainModel> domainResult)
	{
		return switch (domainResult)
		{
			case CreatedCreateOperationResult(var context, var model, var toleratedViolations) ->
					CreateApplicationResults.created(mapContext(context), mapCreatedModel(model),
							mapViolations(toleratedViolations));

			case RejectedCreateOperationResult(var context, var blockingViolations, var toleratedViolations) ->
					CreateApplicationResults.rejected(
							mapContext(context),
							mapViolations(blockingViolations),
							mapViolations(toleratedViolations));

			case QuarantinedCreateOperationResult(
					var context, var blockingViolations, var toleratedViolations, var quarantineReference
			) -> CreateApplicationResults.quarantined(
					mapContext(context),
					mapViolations(blockingViolations),
					mapViolations(toleratedViolations),
					quarantineReference);
		};
	}

	protected abstract APIModel mapCreatedModel(DomainModel domainModel);

	private CreateApplicationViolation mapViolation(final CreationOperationViolation violation)
	{
		return new CreateApplicationViolation(
				CreateApplicationViolationKind.valueOf(violation.kind().name()), violation.message());
	}

	private List<CreateApplicationViolation> mapViolations(final Collection<CreationOperationViolation> violations)
	{
		return violations.stream()
		                 .map(this::mapViolation)
		                 .toList();
	}

	private CreateApplicationResultContext mapContext(final CreationOperationContext context)
	{
		return new CreateApplicationResultContext(
				context.source(),
				context.payloadTypeName(),
				context.correlationId(),
				context.causationId());
	}
}