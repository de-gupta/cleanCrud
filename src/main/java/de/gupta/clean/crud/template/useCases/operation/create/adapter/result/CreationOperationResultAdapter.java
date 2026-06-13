package de.gupta.clean.crud.template.useCases.operation.create.adapter.result;

import de.gupta.clean.crud.template.useCases.operation.create.api.result.CreateAPIResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResult;

@FunctionalInterface
public interface CreationOperationResultAdapter<DomainModel, APIModel>
{
	CreateAPIResult<APIModel> mapToAPIResult(CreationOperationResult<DomainModel> domainResult);
}
