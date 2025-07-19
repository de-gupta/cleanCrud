package de.gupta.clean.crud.template.useCases.crud.save.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.model.exceptions.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.exceptions.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

public abstract class AbstractSaveService<DomainModel, DomainModelCreate, DomainModelResponse, DomainID>
		implements SaveService<DomainModelCreate, DomainModelResponse, DomainID>
{
	private final SavePersistenceService<DomainID, DomainModel> persistenceService;
	private final DomainModelBuilder<DomainModelCreate, DomainModel> modelBuilder;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> responseModelMapper;
	private final InsertionPolicy<DomainModel> insertionPolicy;
	private final DomainSecurityPolicy<DomainModel> domainSecurityPolicy;

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> save(final DomainModelCreate model)
	{
		final var domainModel = modelBuilder.toModel(model);
		if (!domainSecurityPolicy.isAccessAllowed(domainModel))
			throw AccessDeniedException.withMessage("Access not allowed");

		insertionPolicy.validateInsertion(domainModel);
		return identifiedModel(persistenceService.save(domainModel));
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> saveAll(
			final Collection<DomainModelCreate> models)
	{
		var domainModels = models.stream().map(modelBuilder::toModel).toList();

		Optional.of(domainModels)
				.filter(modelList -> modelList.stream().allMatch(domainSecurityPolicy::isAccessAllowed))
				.orElseThrow(() -> AccessDeniedException.withMessage("Access not allowed for one or more models"));

		throwIfDuplicatesInCollection(domainModels);
		domainModels.forEach(insertionPolicy::validateInsertion);
		return persistenceService.saveAll(domainModels).stream().map(this::identifiedModel).toList();
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
			final IdentifiedModel<DomainID, DomainModel> identifiedModel)
	{
		return IdentifiedModel.of(identifiedModel.id(), responseModelMapper.toResponse(identifiedModel.model()));
	}

	protected AbstractSaveService(final SavePersistenceService<DomainID, DomainModel> persistenceService,
								  final DomainModelBuilder<DomainModelCreate, DomainModel> modelBuilder,
								  final DomainResponseBuilder<DomainModel, DomainModelResponse> responseModelMapper,
								  final InsertionPolicy<DomainModel> insertionPolicy,
								  final DomainSecurityPolicy<DomainModel> domainSecurityPolicy)
	{
		this.persistenceService = persistenceService;
		this.modelBuilder = modelBuilder;
		this.responseModelMapper = responseModelMapper;
		this.insertionPolicy = insertionPolicy;
		this.domainSecurityPolicy = domainSecurityPolicy;
	}
}