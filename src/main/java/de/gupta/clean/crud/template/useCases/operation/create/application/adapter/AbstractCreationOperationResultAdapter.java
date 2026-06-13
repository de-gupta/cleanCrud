package de.gupta.clean.crud.template.useCases.operation.create.application.adapter;

import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.application.model.CreateAPIResults;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreatedCreationOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.QuarantinedCreationOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.RejectedCreationOperationResult;

public abstract class AbstractCreationOperationResultAdapter<DomainModel, APIModel>
		implements CreationOperationResultAdapter<DomainModel, APIModel>
{
	@Override
	public CreateAPIResult<APIModel> mapToAPIResult(final CreationOperationResult<DomainModel> domainResult)
	{
		return switch (domainResult)
		{
			case CreatedCreationOperationResult(var model) -> CreateAPIResults.created(mapCreatedModel(model));
			case RejectedCreationOperationResult<DomainModel> _ -> CreateAPIResults.rejected();
			case QuarantinedCreationOperationResult<DomainModel> _ -> CreateAPIResults.quarantined();
		};
	}

	protected abstract APIModel mapCreatedModel(DomainModel domainModel);
}