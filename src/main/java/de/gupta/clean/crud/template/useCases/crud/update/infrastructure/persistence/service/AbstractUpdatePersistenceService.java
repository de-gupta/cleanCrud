package de.gupta.clean.crud.template.useCases.crud.update.infrastructure.persistence.service;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.UnexpectedResourceException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter.DomainPersistenceIDAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter.DomainPersistenceIDManagement;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.model.DomainPersistenceModelAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.service.FetchPersistenceModelRepository;
import de.gupta.clean.crud.template.useCases.crud.save.infrastructure.persistence.service.SavePersistenceModelRepository;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.UpdatePersistenceService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class AbstractUpdatePersistenceService<DomainID, DomainModel,
		PersistenceID, PersistenceModel extends WithID<PersistenceID>>
		implements UpdatePersistenceService<DomainID, DomainModel>
{
	private final FetchPersistenceModelRepository<PersistenceModel, PersistenceID> fetchRepository;
	private final SavePersistenceModelRepository<PersistenceModel> saveRepository;
	private final UpdatePersistenceModelRepository<PersistenceModel> updateRepository;
	private final DomainPersistenceModelAdapter<DomainModel, PersistenceModel> modelAdapter;
	private final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter;
	private final DomainPersistenceIDManagement<DomainID, PersistenceID> idAdapterService;
	private final PersistenceTransactionRunner transactionRunner;

	@Override
	public void putAtId(final DomainID id, final DomainModel model)
	{
		transactionRunner.inTransaction(() ->
		{
			final Optional<PersistenceID> originalID = idAdapter.toPersistenceID(id);

			originalID.ifPresentOrElse(persistenceId ->
			{
				final PersistenceModel updatedModel = updateRepository.update(
						preparePersistenceModelForUpdate(id, model));
				if (!persistenceId.equals(updatedModel.id()))
				{
					throw UnexpectedResourceException.withMessage(
							"Persistence ID changed during PUT for domain ID " + id +
									": expected " + persistenceId + " but got " + updatedModel.id());
				}
			}, () -> save(id, model));
		});
	}

	@Override
	public IdentifiedModel<DomainID, DomainModel> updateById(final DomainID id, final DomainModel model)
	{
		return transactionRunner.inTransaction(
				() -> identifiedModel(updateRepository.update(preparePersistenceModelForUpdate(id, model))));
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModel>> updateAllById(
			final Collection<IdentifiedModel<DomainID, DomainModel>> models)
	{
		if (models.isEmpty())
		{
			return List.of();
		}

		return transactionRunner.inTransaction(() ->
				updateRepository.updateAll(models.stream()
				                                 .map(model -> preparePersistenceModelForUpdate(model.id(),
														 model.model()))
				                                 .toList())
				                .stream()
				                .map(this::identifiedModel)
				                .toList());
	}

	private void save(final DomainID domainID, final DomainModel entity)
	{
		PersistenceModel savedModel = saveRepository.save(modelAdapter.toPersistenceModel(entity));
		PersistenceID savedID = savedModel.id();
		idAdapterService.update(domainID, savedID);
	}

	private PersistenceModel preparePersistenceModelForUpdate(final DomainID id, final DomainModel model)
	{
		PersistenceID persistenceID = idAdapter.toPersistenceID(id)
		                                       .orElseThrow(() -> ResourceNotFoundException.withId(id));
		PersistenceModel originalPersistenceModel = fetchRepository.findById(persistenceID)
		                                                           .orElseThrow(
																		   () -> ResourceNotFoundException.withId(id));
		return patchModel(originalPersistenceModel, model);
	}

	private PersistenceModel patchModel(final PersistenceModel originalModel, final DomainModel updatedDomainModel)
	{
		return modelAdapter.updatePersistenceModel(originalModel, updatedDomainModel);
	}

	private IdentifiedModel<DomainID, DomainModel> identifiedModel(
			final DomainID domainID,
			final PersistenceModel persistenceModel)
	{
		return IdentifiedModel.of(domainID, modelAdapter.toDomainModel(persistenceModel));
	}

	private IdentifiedModel<DomainID, DomainModel> identifiedModel(final PersistenceModel persistenceModel)
	{
		return IdentifiedModel.of(idAdapter.toDomainID(persistenceModel.id())
		                                   .orElseThrow(() -> ResourceNotFoundException.withMessage("There was an " +
												   "issue with one of the updated models")),
				modelAdapter.toDomainModel(persistenceModel));
	}

	protected AbstractUpdatePersistenceService(
			final FetchPersistenceModelRepository<PersistenceModel, PersistenceID> fetchRepository,
			final SavePersistenceModelRepository<PersistenceModel> saveRepository,
			final UpdatePersistenceModelRepository<PersistenceModel> updateRepository,
			final DomainPersistenceModelAdapter<DomainModel, PersistenceModel> modelAdapter,
			final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter,
			final DomainPersistenceIDManagement<DomainID, PersistenceID> idAdapterService,
			final PersistenceTransactionRunner transactionRunner)
	{
		this.fetchRepository = fetchRepository;
		this.saveRepository = saveRepository;
		this.updateRepository = updateRepository;
		this.modelAdapter = modelAdapter;
		this.idAdapter = idAdapter;
		this.idAdapterService = idAdapterService;
		this.transactionRunner = transactionRunner;
	}
}