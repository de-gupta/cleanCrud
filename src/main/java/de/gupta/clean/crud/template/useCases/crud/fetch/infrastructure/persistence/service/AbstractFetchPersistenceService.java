package de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter.DomainPersistenceIDAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.model.DomainPersistenceModelAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractFetchPersistenceService<DomainID, DomainModel, PersistenceID, PersistenceModel extends WithID<PersistenceID>>
		implements FetchPersistenceService<DomainID, DomainModel>
{
	private final FetchPersistenceModelRepository<PersistenceModel, PersistenceID> repository;
	private final DomainPersistenceModelAdapter<DomainModel, PersistenceModel> modelAdapter;
	private final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModel>> findAll()
	{
		return repository.findAll().stream().map(this::identifiedModel).flatMap(Optional::stream).toList();
	}

	@Override
	public Page<IdentifiedModel<DomainID, DomainModel>> findAll(final Pageable pageable)
	{
		Page<? extends PersistenceModel> persistenceModelPage = repository.findAll(pageable);
		return PageUtility.mapPageOptionallyAndFilter(persistenceModelPage, this::identifiedModel);
	}

	@Override
	public Optional<IdentifiedModel<DomainID, DomainModel>> findById(final DomainID domainID)
	{
		return idAdapter.toPersistenceID(domainID).flatMap(repository::findById).flatMap(this::identifiedModel);
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModel>> findByIds(final Set<DomainID> IDs)
	{
		final var persistenceIDs =
				IDs.stream().map(idAdapter::toPersistenceID).filter(Optional::isPresent).map(Optional::get).toList();
		return repository.findByIds(persistenceIDs).stream().map(this::identifiedModel).flatMap(Optional::stream)
						 .toList();
	}

	private Optional<IdentifiedModel<DomainID, DomainModel>> identifiedModel(final PersistenceModel persistenceModel)
	{
		return idAdapter.toDomainID(persistenceModel.id())
						.map(id -> IdentifiedModel.of(id, modelAdapter.toDomainModel(persistenceModel)));
	}

	protected AbstractFetchPersistenceService(
			final FetchPersistenceModelRepository<PersistenceModel, PersistenceID> repository,
			final DomainPersistenceModelAdapter<DomainModel, PersistenceModel> modelAdapter,
			final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter)
	{
		this.repository = repository;
		this.modelAdapter = modelAdapter;
		this.idAdapter = idAdapter;
	}
}