package de.gupta.clean.crud.template.useCases.crud.all.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

@Deprecated
public interface ApplicationCrudController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	Page<WebModelResponse> findAll(Pageable pageable);

	WebModelResponse findById(final WebModelID id);

	WebModelResponse save(final WebModelCreate model);

	Collection<WebModelResponse> saveAll(final Collection<WebModelCreate> models);

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

	void deleteById(final WebModelID id);

	default void deleteAllById(final Collection<WebModelID> ids)
	{
		deleteAllById(ids, BulkOperationMode.ALL_OR_NOTHING);
	}

	void deleteAllById(final Collection<WebModelID> ids, final BulkOperationMode mode);
}
