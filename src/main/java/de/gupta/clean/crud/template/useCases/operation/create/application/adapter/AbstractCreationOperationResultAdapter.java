package de.gupta.clean.crud.template.useCases.operation.create.application.adapter;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIResults;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIViolation;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIViolationKind;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.*;

import java.util.Collection;
import java.util.List;

public abstract class AbstractCreationOperationResultAdapter<DomainModel, APIModel>
		implements CreationOperationResultAdapter<DomainModel, APIModel>
{
	@Override
	public CreateAPIResult<APIModel> mapToAPIResult(final CreationOperationResult<DomainModel> domainResult)
	{
		return switch (domainResult)
		{
			case CreatedCreationOperationResult(var model, var toleratedViolations) ->
					CreateAPIResults.created(mapCreatedModel(model), mapViolations(toleratedViolations));

			case RejectedCreationOperationResult(var blockingViolations, var toleratedViolations) ->
					CreateAPIResults.rejected(mapViolations(blockingViolations), mapViolations(toleratedViolations));

			case QuarantinedCreationOperationResult(
					var blockingViolations, var toleratedViolations, var quarantineReference
			) -> CreateAPIResults.quarantined(
					mapViolations(blockingViolations),
					mapViolations(toleratedViolations),
					quarantineReference);
		};
	}

	protected abstract APIModel mapCreatedModel(DomainModel domainModel);

	private CreateAPIViolation mapViolation(final CreationOperationViolation violation)
	{
		return new CreateAPIViolation(CreateAPIViolationKind.valueOf(violation.kind().name()), violation.message());
	}

	private List<CreateAPIViolation> mapViolations(final Collection<CreationOperationViolation> violations)
	{
		return violations.stream()
		                 .map(this::mapViolation)
		                 .toList();
	}
}