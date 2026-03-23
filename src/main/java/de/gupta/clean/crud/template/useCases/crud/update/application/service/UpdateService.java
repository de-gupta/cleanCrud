package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;

import java.util.Collection;

public interface UpdateService<DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID>
{
	void putAtId(final DomainID id, final DomainModelCreate model);

	IdentifiedModel<DomainID, DomainModelResponse> updateById(final DomainID id, final DomainModelUpdatePatch model);

	default Collection<IdentifiedModel<DomainID, DomainModelResponse>> updateAllById(
			final Collection<IdentifiedModel<DomainID, DomainModelUpdatePatch>> models)
	{
		return updateAllById(models, BulkOperationMode.ALL_OR_NOTHING);
	}

	Collection<IdentifiedModel<DomainID, DomainModelResponse>> updateAllById(
			final Collection<IdentifiedModel<DomainID, DomainModelUpdatePatch>> models,
			final BulkOperationMode mode);
}
