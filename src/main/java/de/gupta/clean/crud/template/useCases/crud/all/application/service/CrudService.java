package de.gupta.clean.crud.template.useCases.crud.all.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
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

	void deleteById(final DomainID id);
}