package de.gupta.clean.crud.template.useCases.operation.domain.metadata.result;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

public interface CreationOperationResult<ModelID, ModelResponse>
{
	IdentifiedModel<ModelID, ModelResponse> model(); //either directly a model or a supplier of model etc. - details to be specified later

	String metadata(); // placeholder, richer metadata or other failure modes etc can be added later
}