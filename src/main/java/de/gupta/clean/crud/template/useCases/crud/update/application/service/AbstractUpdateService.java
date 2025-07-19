package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;

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
							original -> validateAndPatch(original.model(), newDomainModel),
							() -> insertionPolicy.validateInsertion(newDomainModel)
					);

		persistenceService.putAtId(id, newDomainModel);
	}

	private void validateAccess(DomainModel model)
	{
		if (!domainSecurityPolicy.isAccessAllowed(model))
			throw new IllegalArgumentException("Access not allowed");
	}

	private void validateAndPatch(DomainModel original, DomainModel newModel)
	{
		validateAccess(original);
		patchPolicy.validatePatchAttempt(original, newModel);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> updateById(final DomainID id,
																	 final DomainModelUpdatePatch updatePatch)
	{
		var originalModel = persistenceService.findById(id)
											  .map(IdentifiedModel::model)
											  .orElseThrow(() -> ResourceNotFoundException.withId(id));
		validateAccess(originalModel);

		var updatedModel = modelPatcher.patchModel(originalModel, updatePatch);
		validateAccess(updatedModel);

		patchPolicy.validatePatchAttempt(originalModel, updatedModel);

		return identifiedModel(persistenceService.updateById(id, updatedModel));
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