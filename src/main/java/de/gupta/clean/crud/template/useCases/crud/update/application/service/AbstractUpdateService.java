package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.DomainException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractUpdateService<DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID>
		implements UpdateService<DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID>
{
	private final FetchPersistenceService<DomainID, DomainModel> fetchService;
	private final UpdatePersistenceService<DomainID, DomainModel> persistenceService;
	private final DomainModelBuilder<DomainModelCreate, DomainModel> createModelBuilder;
	private final DomainModelPatcher<DomainModel, DomainModelUpdatePatch> modelPatcher;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> responseModelMapper;
	private final InsertionPolicy<DomainModel> insertionPolicy;
	private final PatchPolicy<DomainModel> patchPolicy;
	private final DomainSecurityPolicy<DomainModel> domainSecurityPolicy;

	@Override
	public void putAtId(final DomainID id, final DomainModelCreate model)
	{
		var newDomainModel = createModelBuilder.toModel(model);

		validateAccess(newDomainModel);

		fetchService.findById(id)
		            .ifPresentOrElse(
							original -> validateAccessAndValidatePatch(original.model(), newDomainModel),
							() -> insertionPolicy.validateInsertion(newDomainModel)
					);

		persistenceService.putAtId(id, newDomainModel);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> updateById(final DomainID id,
	                                                                 final DomainModelUpdatePatch updatePatch)
	{
		return identifiedModel(persistenceService.updateById(id, prepareUpdatedModel(id, updatePatch).model()));
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> updateAllById(
			final Collection<IdentifiedModel<DomainID, DomainModelUpdatePatch>> models,
			final BulkOperationMode mode)
	{
		Collection<IdentifiedModel<DomainID, DomainModel>> preparedModels = switch (mode)
		{
			case ALL_OR_NOTHING -> models.stream()
			                             .map(model -> prepareUpdatedModel(model.id(), model.model()))
			                             .toList();
			case BEST_EFFORT -> models.stream()
			                          .map(model -> tryPrepareUpdatedModel(model.id(), model.model()))
			                          .flatMap(Optional::stream)
			                          .toList();
		};

		return persistenceService.updateAllById(preparedModels)
		                         .stream()
		                         .map(this::identifiedModel)
		                         .toList();
	}

	private IdentifiedModel<DomainID, DomainModel> prepareUpdatedModel(
			final DomainID id,
			final DomainModelUpdatePatch updatePatch)
	{
		var originalModel = fetchService.findById(id)
		                                .map(IdentifiedModel::model)
		                                .orElseThrow(() -> ResourceNotFoundException.withId(id));
		validateAccess(originalModel);

		var updatedModel = modelPatcher.patchModel(originalModel, updatePatch);
		validateAccessAndValidatePatch(originalModel, updatedModel);
		return IdentifiedModel.of(id, updatedModel);
	}

	private Optional<IdentifiedModel<DomainID, DomainModel>> tryPrepareUpdatedModel(
			final DomainID id,
			final DomainModelUpdatePatch updatePatch)
	{
		try
		{
			return Optional.of(prepareUpdatedModel(id, updatePatch));
		}
		catch (DomainException e)
		{
			return Optional.empty();
		}
	}

	private void validateAccess(DomainModel model)
	{
		if (!domainSecurityPolicy.isAccessAllowed(model))
			throw AccessDeniedException.withMessage("Access not allowed");
	}

	private void validateAccessAndValidatePatch(DomainModel original, DomainModel newModel)
	{
		validateAccess(original);
		validateAccess(newModel);
		patchPolicy.validatePatchAttempt(original, newModel);
	}

	private IdentifiedModel<DomainID, DomainModelResponse> identifiedModel(
			final IdentifiedModel<DomainID, DomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), responseModelMapper.toResponse(domainModel.model()));
	}

	protected AbstractUpdateService(
			final FetchPersistenceService<DomainID, DomainModel> fetchService,
			final UpdatePersistenceService<DomainID, DomainModel> persistenceService,
			final DomainModelBuilder<DomainModelCreate, DomainModel> createModelBuilder,
			final DomainModelPatcher<DomainModel, DomainModelUpdatePatch> modelPatcher,
			final DomainResponseBuilder<DomainModel, DomainModelResponse> responseModelMapper,
			final InsertionPolicy<DomainModel> insertionPolicy, final PatchPolicy<DomainModel> patchPolicy,
			final DomainSecurityPolicy<DomainModel> domainSecurityPolicy)
	{
		this.fetchService = fetchService;
		this.persistenceService = persistenceService;
		this.createModelBuilder = createModelBuilder;
		this.modelPatcher = modelPatcher;
		this.responseModelMapper = responseModelMapper;
		this.insertionPolicy = insertionPolicy;
		this.patchPolicy = patchPolicy;
		this.domainSecurityPolicy = domainSecurityPolicy;
	}
}