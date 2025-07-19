package de.gupta.clean.crud.template.useCases.crud.save.infrastructure.persistence.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter.DomainPersistenceIDManagement;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.model.DomainPersistenceModelAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.SavePersistenceService;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractSavePersistenceService<DomainID, DomainModel,
		PersistenceID, PersistenceModel extends WithID<PersistenceID>>
		implements SavePersistenceService<DomainID, DomainModel>
{
	private final SavePersistenceModelRepository<PersistenceModel> repository;
	private final DomainPersistenceModelAdapter<DomainModel, PersistenceModel> modelAdapter;
	private final DomainPersistenceIDManagement<DomainID, PersistenceID> idManagement;

	@Override
	public IdentifiedModel<DomainID, DomainModel> save(final DomainModel model)
	{
		PersistenceModel persistenceModel = repository.save(modelAdapter.toPersistenceModel(model));
		PersistenceID persistenceID = persistenceModel.id();
		DomainID domainID = idManagement.add(persistenceID);
		return IdentifiedModel.of(domainID, model);
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModel>> saveAll(final Collection<DomainModel> models)
	{
		Collection<PersistenceModel> persistenceModels = models.stream()
															   .map(modelAdapter::toPersistenceModel)
															   .collect(Collectors.toList());

		var savedModels = repository.saveAll(persistenceModels);

		Map<PersistenceID, DomainID> idMap = idManagement.addBatch(savedModels.stream().map(WithID::id).toList());

		return savedModels.stream()
						  .map(savedModel -> identifiedModel(savedModel, idMap))
						  .toList();
	}

	private IdentifiedModel<DomainID, DomainModel> identifiedModel(final PersistenceModel persistenceModel,
																   final Map<PersistenceID, DomainID> idMap)
	{
		return IdentifiedModel.of(idMap.get(persistenceModel.id()), modelAdapter.toDomainModel(persistenceModel));
	}

	protected AbstractSavePersistenceService(
			final SavePersistenceModelRepository<PersistenceModel> repository,
			final DomainPersistenceModelAdapter<DomainModel, PersistenceModel> modelAdapter,
			final DomainPersistenceIDManagement<DomainID, PersistenceID> idManagement)
	{
		this.repository = repository;
		this.modelAdapter = modelAdapter;
		this.idManagement = idManagement;
	}
}