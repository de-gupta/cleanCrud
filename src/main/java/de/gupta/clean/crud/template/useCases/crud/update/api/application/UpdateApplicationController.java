package de.gupta.clean.crud.template.useCases.crud.update.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;

import java.util.Collection;

public interface UpdateApplicationController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	void putAtId(final WebModelID id, final WebModelCreate model);

	WebModelResponse updateById(final WebModelID id, final WebModelUpdatePatch model);

	default Collection<WebModelResponse> updateAllById(
			final Collection<IdentifiedModel<WebModelID, WebModelUpdatePatch>> models)
	{
		return updateAllById(models, BulkOperationMode.ALL_OR_NOTHING);
	}

	Collection<WebModelResponse> updateAllById(
			final Collection<IdentifiedModel<WebModelID, WebModelUpdatePatch>> models,
			final BulkOperationMode mode);
}
