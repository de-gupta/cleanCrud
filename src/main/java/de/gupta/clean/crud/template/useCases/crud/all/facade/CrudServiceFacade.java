package de.gupta.clean.crud.template.useCases.crud.all.facade;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface CrudServiceFacade<APIModelCreate, APIModelUpdatePatch, APIModelResponse, APIModelID>
{
	Collection<APIModelResponse> findAll();

	Page<APIModelResponse> findAll(final Pageable pageable);

	APIModelResponse findById(final APIModelID id);

	APIModelResponse save(final APIModelCreate model);

	Collection<APIModelResponse> saveAll(final Collection<APIModelCreate> models);

	void putAtId(final APIModelID id, final APIModelCreate model);

	APIModelResponse updateById(final APIModelID id, final APIModelUpdatePatch updatePatch);

	default Collection<APIModelResponse> updateAllById(
			final Collection<IdentifiedModel<APIModelID, APIModelUpdatePatch>> models)
	{
		return updateAllById(models, BulkOperationMode.ALL_OR_NOTHING);
	}

	Collection<APIModelResponse> updateAllById(
			final Collection<IdentifiedModel<APIModelID, APIModelUpdatePatch>> models,
			final BulkOperationMode mode);

	void deleteById(final APIModelID id);

	default void deleteAllById(final Collection<APIModelID> ids)
	{
		deleteAllById(ids, BulkOperationMode.ALL_OR_NOTHING);
	}

	void deleteAllById(final Collection<APIModelID> ids, final BulkOperationMode mode);
}
