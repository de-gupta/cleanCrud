package de.gupta.clean.crud.template.useCases.crud.save.application.service;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DomainEqualityPolicy;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractSaveService<DomainModel, DomainModelCreate, DomainModelResponse, DomainID>
		implements SaveService<DomainModelCreate, DomainModelResponse, DomainID>
{
	private final SavePersistenceService<DomainID, DomainModel> persistenceService;
	private final DomainModelBuilder<DomainModelCreate, DomainModel> modelBuilder;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> responseModelMapper;
	private final InsertionPolicy<DomainModel> insertionPolicy;
	private final DomainSecurityPolicy<DomainModel> domainSecurityPolicy;
	private final DomainEqualityPolicy<DomainModel> domainEqualityPolicy;

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> save(final DomainModelCreate model)
	{
		return saveAll(List.of(model)).stream().findFirst().orElseThrow();
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> saveAll(
			final Collection<DomainModelCreate> models)
	{
		var domainModels = models.stream().map(modelBuilder::toModel).toList();
		validateDomainModels(domainModels);
		return persistenceService.saveAll(domainModels).stream().map(this::identifiedModel).toList();
	}

	private void validateDomainModels(final Collection<DomainModel> domainModels)
	{
		Unfolding.beckon(domainModels)
				 .discern(m -> m.stream().allMatch(domainSecurityPolicy::isAccessAllowed),
						 () -> AccessDeniedException.withMessage("Access not allowed for one or more models"))
				 .unlace(this::throwIfDuplicatesInCollection)
				 .unlace(models -> models.forEach(insertionPolicy::validateInsertion));
	}

	private void throwIfDuplicatesInCollection(final Collection<DomainModel> models)
	{
		var seen = new ArrayList<DomainModel>();

		for (var model : models)
		{
			if (seen.stream().anyMatch(existing -> domainEqualityPolicy.areEqual(existing, model)))
			{
				throw InvalidRequestException.withMessage("The collection contains duplicate elements");
			}
			seen.add(model);
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
								  final DomainSecurityPolicy<DomainModel> domainSecurityPolicy,
								  final DomainEqualityPolicy<DomainModel> domainEqualityPolicy)
	{
		this.persistenceService = persistenceService;
		this.modelBuilder = modelBuilder;
		this.responseModelMapper = responseModelMapper;
		this.insertionPolicy = insertionPolicy;
		this.domainSecurityPolicy = domainSecurityPolicy;
		this.domainEqualityPolicy = domainEqualityPolicy;
	}
}