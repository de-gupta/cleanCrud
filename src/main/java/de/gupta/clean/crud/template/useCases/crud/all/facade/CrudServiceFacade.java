package de.gupta.clean.crud.template.useCases.crud.all.facade;

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

	void deleteById(final APIModelID id);
}