package de.gupta.clean.crud.template.useCases.crud.all.api.application;

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

	void deleteById(final WebModelID id);
}