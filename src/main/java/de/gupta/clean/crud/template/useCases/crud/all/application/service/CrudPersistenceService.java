package de.gupta.clean.crud.template.useCases.crud.all.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;

public interface CrudPersistenceService<DomainID, DomainModel>
{
	Collection<IdentifiedModel<DomainID, DomainModel>> findAll();

	Page<IdentifiedModel<DomainID, DomainModel>> findAll(final Pageable pageable);

	Optional<IdentifiedModel<DomainID, DomainModel>> findById(final DomainID id);

	IdentifiedModel<DomainID, DomainModel> save(final DomainModel model);

	Collection<IdentifiedModel<DomainID, DomainModel>> saveAll(final Collection<DomainModel> models);

	void replaceIfExistsOrSave(final DomainID id, final DomainModel model);

	IdentifiedModel<DomainID, DomainModel> updateById(final DomainID id, final DomainModel patchedModel);

	void deleteById(final DomainID id);
}