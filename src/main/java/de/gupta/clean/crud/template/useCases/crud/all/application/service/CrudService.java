package de.gupta.clean.crud.template.useCases.crud.all.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface CrudService<DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID>
{
	Collection<IdentifiedModel<DomainID, DomainModelResponse>> findAll();

	Page<IdentifiedModel<DomainID, DomainModelResponse>> findAll(final Pageable pageable);

	IdentifiedModel<DomainID, DomainModelResponse> findById(final DomainID id);

	IdentifiedModel<DomainID, DomainModelResponse> save(final DomainModelCreate model);

	Collection<IdentifiedModel<DomainID, DomainModelResponse>> saveAll(final Collection<DomainModelCreate> models);

	void putAtId(final DomainID id, final DomainModelCreate model);

	IdentifiedModel<DomainID, DomainModelResponse> updateById(final DomainID id,
															  final DomainModelUpdatePatch updatePatch);

	default Collection<IdentifiedModel<DomainID, DomainModelResponse>> updateAllById(
			final Collection<IdentifiedModel<DomainID, DomainModelUpdatePatch>> models)
	{
		return updateAllById(models, BulkOperationMode.ALL_OR_NOTHING);
	}

	Collection<IdentifiedModel<DomainID, DomainModelResponse>> updateAllById(
			final Collection<IdentifiedModel<DomainID, DomainModelUpdatePatch>> models,
			final BulkOperationMode mode);

	void deleteById(final DomainID id);

	default void deleteAllById(final Collection<DomainID> ids)
	{
		deleteAllById(ids, BulkOperationMode.ALL_OR_NOTHING);
	}

	void deleteAllById(final Collection<DomainID> ids, final BulkOperationMode mode);
}
