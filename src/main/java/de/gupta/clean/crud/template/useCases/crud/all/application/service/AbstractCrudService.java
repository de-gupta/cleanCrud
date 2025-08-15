package de.gupta.clean.crud.template.useCases.crud.all.application.service;


import de.gupta.clean.crud.template.domain.mapping.CrudDomainModelMapper;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.HashSet;

public abstract class AbstractCrudService<DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse,
		DomainID, DomainModel>
		implements CrudService<DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID>
{
	private final CrudPersistenceService<DomainID, DomainModel> persistenceService;
	private final CrudDomainModelMapper<DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
			modelMapper;
	private final InsertionPolicy<DomainModel> insertionPolicy;
	private final DeletionPolicy<DomainModel> deletionPolicy;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> findAll()
	{
		return persistenceService.findAll()
								 .stream()
								 .map(this::identifiedModel)
								 .toList();
	}

	@Override
	public Page<IdentifiedModel<DomainID, DomainModelResponse>> findAll(final Pageable pageable)
	{
		return PageUtility.mapPage(persistenceService.findAll(pageable), this::identifiedModel);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> findById(final DomainID domainID)
	{
		return persistenceService.findById(domainID)
								 .map(this::identifiedModel)
								 .orElseThrow(() -> ResourceNotFoundException.withId(domainID));
	}

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> save(final DomainModelCreate model)
	{
		var domainModel = modelMapper.toModel(model);
		insertionPolicy.validateInsertion(domainModel);
		return identifiedModel(persistenceService.save(domainModel));
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> saveAll(
			final Collection<DomainModelCreate> models)
	{
		var domainModels = models.stream().map(modelMapper::toModel).toList();
		throwIfDuplicatesInCollection(domainModels);
		domainModels.forEach(insertionPolicy::validateInsertion);
		return persistenceService.saveAll(domainModels).stream().map(this::identifiedModel)
								 .toList();
	}

	@Override
	public void putAtId(final DomainID domainID, final DomainModelCreate model)
	{
		var domainModel = modelMapper.toModel(model);
		insertionPolicy.validateInsertion(domainModel);
		persistenceService.replaceIfExistsOrSave(domainID, domainModel);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> updateById(final DomainID domainID,
																	 final DomainModelUpdatePatch updatePatch)
	{
		var originalModel = persistenceService.findById(domainID)
											  .map(IdentifiedModel::model)
											  .orElseThrow(() -> ResourceNotFoundException.withId(domainID));
		var updatedModel = modelMapper.patchModel(originalModel, updatePatch);

		if (originalModel.equals(updatedModel))
		{
			return identifiedModel(persistenceService.updateById(domainID, updatedModel));
		}

		insertionPolicy.validateInsertion(updatedModel);
		return identifiedModel(persistenceService.updateById(domainID, updatedModel));
	}

	@Override
	public void deleteById(final DomainID domainID)
	{
		deletionPolicy.validateDeletion(persistenceService.findById(domainID)
														  .map(IdentifiedModel::model)
														  .orElseThrow(
																  () -> ResourceNotFoundException.withId(domainID)));
		persistenceService.deleteById(domainID);
	}

	private void throwIfDuplicatesInCollection(final Collection<DomainModel> models)
	{
		var seen = new HashSet<DomainModel>();

		for (var model : models)
		{
			if (!seen.add(model))
			{
				throw InvalidRequestException.withMessage("The collection contains duplicate elements");
			}
		}
	}

	private IdentifiedModel<DomainID, DomainModelResponse> identifiedModel(
			final IdentifiedModel<DomainID, DomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), modelMapper.toResponse(domainModel.model()));
	}

	protected AbstractCrudService(
			final CrudPersistenceService<DomainID, DomainModel> persistenceService,
			final CrudDomainModelMapper<DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> modelMapper,
			final InsertionPolicy<DomainModel> insertionPolicy,
			final DeletionPolicy<DomainModel> deletionPolicy)
	{
		this.persistenceService = persistenceService;
		this.modelMapper = modelMapper;
		this.insertionPolicy = insertionPolicy;
		this.deletionPolicy = deletionPolicy;
	}
}