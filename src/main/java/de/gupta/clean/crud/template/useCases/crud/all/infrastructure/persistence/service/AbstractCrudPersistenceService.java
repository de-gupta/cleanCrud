package de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.service;

import de.gupta.clean.crud.template.domain.model.exceptions.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter.DomainPersistenceIDAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter.DomainPersistenceIDManagement;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.model.DomainPersistenceModelAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.all.application.service.CrudPersistenceService;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractCrudPersistenceService<DomainID, DomainModel,
		PersistenceID, PersistenceModel extends WithID<PersistenceID>>
		implements CrudPersistenceService<DomainID, DomainModel>
{
	private final PersistenceModelCrudRepository<PersistenceModel, PersistenceID> repository;
	private final DomainPersistenceModelAdapter<DomainModel, PersistenceModel> modelAdapter;
	private final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter;
	private final DomainPersistenceIDManagement<DomainID, PersistenceID> idAdapterService;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModel>> findAll()
	{
		return repository.findAll()
						 .stream()
						 .map(this::identifiedModel)
						 .toList();
	}

	@Override
	public Page<IdentifiedModel<DomainID, DomainModel>> findAll(final Pageable pageable)
	{
		Page<? extends PersistenceModel> persistenceModelPage = repository.findAll(pageable);
		return PageUtility.mapPage(persistenceModelPage, this::identifiedModel);
	}

	@Override
	public Optional<IdentifiedModel<DomainID, DomainModel>> findById(final DomainID domainID)
	{
		return idAdapter.toPersistenceID(domainID)
						.flatMap(repository::findById)
						.map(this::identifiedModel);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModel> save(final DomainModel model)
	{
		PersistenceModel savedModel = repository.save(modelAdapter.toPersistenceModel(model));
		PersistenceID savedID = savedModel.id();
		DomainID domainID = idAdapterService.add(savedID);
		DomainModel domainModel = modelAdapter.toDomainModel(savedModel);
		return IdentifiedModel.of(domainID, domainModel);
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModel>> saveAll(final Collection<DomainModel> models)
	{
		Collection<PersistenceModel> persistenceModels = models.stream()
															   .map(modelAdapter::toPersistenceModel)
															   .toList();

		Collection<? extends PersistenceModel> savedModels = repository.saveAll(persistenceModels);

		var persistenceIDs = savedModels.stream()
										.map(PersistenceModel::id)
										.toList();

		Map<PersistenceID, DomainID> domainIDMap = idAdapterService.addBatch(persistenceIDs);

		return savedModels.stream()
						  .map(savedModel ->
						  {
							  PersistenceID savedID = savedModel.id();
							  DomainID domainID = domainIDMap.get(savedID);
							  DomainModel domainModel = modelAdapter.toDomainModel(savedModel);
							  return IdentifiedModel.of(domainID, domainModel);
						  })
						  .toList();
	}

	@Override
	public void replaceIfExistsOrSave(final DomainID domainID, final DomainModel model)
	{
		final Optional<PersistenceID> originalID = idAdapter.toPersistenceID(domainID);

		originalID.ifPresentOrElse(id ->
		{
			final PersistenceModel newModel = repository.save(modelAdapter.toPersistenceModel(model));
			final PersistenceID newID = newModel.id();
			if (!id.equals(newID))
			{
				idAdapterService.update(domainID, newID);
			}
		}, () -> save(domainID, model));
	}

	private void save(final DomainID domainID, final DomainModel entity)
	{
		PersistenceModel savedModel = repository.save(modelAdapter.toPersistenceModel(entity));
		PersistenceID savedID = savedModel.id();
		idAdapterService.update(domainID, savedID);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModel> updateById(final DomainID domainID, final DomainModel patchedModel)
	{
		final Optional<PersistenceID> originalID = idAdapter.toPersistenceID(domainID);
		if (originalID.isEmpty())
		{
			throw ResourceNotFoundException.withId(domainID);
		}
		PersistenceModel originalPersistenceModel = repository.findById(originalID.get())
															  .orElseThrow(
																	  () -> ResourceNotFoundException.withId(domainID));
		repository.save(patchModel(originalPersistenceModel, patchedModel));
		return IdentifiedModel.of(domainID, patchedModel);
	}

	@Override
	public void deleteById(final DomainID domainID)
	{
		idAdapter.toPersistenceID(domainID).ifPresentOrElse(this::deleteByPersistenceId, () ->
		{
			throw ResourceNotFoundException.withId(domainID);
		});
	}

	private void deleteByPersistenceId(final PersistenceID persistenceID)
	{
		if (!repository.existsById(persistenceID))
		{
			throw ResourceNotFoundException.withId(persistenceID);
		}
		repository.deleteById(persistenceID);
	}

	private PersistenceModel patchModel(final PersistenceModel originalModel, final DomainModel updatedDomainModel)
	{
		return modelAdapter.updatePersistenceModel(originalModel, updatedDomainModel);
	}

	private IdentifiedModel<DomainID, DomainModel> identifiedModel(final PersistenceModel persistenceModel)
	{
		return IdentifiedModel.of(idAdapter.toDomainID(persistenceModel.id())
										   .orElseThrow(() -> ResourceNotFoundException.withMessage("There was an " +
												   "issue with one of the saved models")),
				modelAdapter.toDomainModel(persistenceModel));
	}

	protected AbstractCrudPersistenceService(
			final PersistenceModelCrudRepository<PersistenceModel, PersistenceID> repository,
			final DomainPersistenceModelAdapter<DomainModel, PersistenceModel> modelAdapter,
			final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter,
			final DomainPersistenceIDManagement<DomainID, PersistenceID> idAdapterService)
	{
		this.repository = repository;
		this.modelAdapter = modelAdapter;
		this.idAdapter = idAdapter;
		this.idAdapterService = idAdapterService;
	}
}