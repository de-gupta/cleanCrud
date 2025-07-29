package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.exceptions.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractFetchService<DomainModelResponse, DomainID, DomainModel>
		implements FetchService<DomainModelResponse, DomainID>
{
	private final FetchPersistenceService<DomainID, DomainModel> persistenceService;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> modelMapper;
	private final DomainFilterPipeline<DomainModel> baseFilterPipeline;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> findAll()
	{
		return persistenceService.findAll()
								 .stream()
								 .filter(this::isVisible)
								 .map(this::respond)
								 .toList();
	}

	@Override
	public Page<IdentifiedModel<DomainID, DomainModelResponse>> findAll(final Pageable pageable)
	{
		return PageUtility.mapPage(
				PageUtility.filterPage(persistenceService.findAll(pageable), this::isVisible),
				this::respond);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> findById(final DomainID domainID)
	{
		return persistenceService.findById(domainID)
								 .filter(this::isVisible)
								 .map(this::respond)
								 .orElseThrow(() -> ResourceNotFoundException.withId(domainID));
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> findByIds(final Set<DomainID> IDs)
	{
		return persistenceService.findByIds(IDs).stream()
								 .filter(this::isVisible)
								 .map(this::respond)
								 .toList();
	}

	private boolean isVisible(IdentifiedModel<DomainID, DomainModel> identifiedModel)
	{
		return baseFilterPipeline.allows(identifiedModel.model());
	}

	private IdentifiedModel<DomainID, DomainModelResponse> respond(
			final IdentifiedModel<DomainID, DomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), modelMapper.toResponse(domainModel.model()));
	}

	protected Collection<IdentifiedModel<DomainID, DomainModelResponse>> filterAndMap(
			Collection<IdentifiedModel<DomainID, DomainModel>> input,
			DomainFilterPipeline<DomainModel> additionalFilter)
	{
		return input.stream()
					.filter(im -> baseFilterPipeline.and(additionalFilter).allows(im.model()))
					.map(this::respond)
					.toList();
	}

	protected Collection<IdentifiedModel<DomainID, DomainModel>> findAllRaw()
	{
		return persistenceService.findAll();
	}

	protected Collection<IdentifiedModel<DomainID, DomainModel>> findByIdsRaw(Set<DomainID> ids)
	{
		return persistenceService.findByIds(ids);
	}

	protected Optional<IdentifiedModel<DomainID, DomainModel>> findByIdRaw(DomainID id)
	{
		return persistenceService.findById(id);
	}

	protected AbstractFetchService(
			final FetchPersistenceService<DomainID, DomainModel> persistenceService,
			final DomainResponseBuilder<DomainModel, DomainModelResponse> modelMapper,
			final DomainSecurityPolicy<DomainModel> domainSecurityPolicy)
	{
		this.persistenceService = persistenceService;
		this.modelMapper = modelMapper;
		this.baseFilterPipeline = DomainFilterPipeline.of(domainSecurityPolicy::isAccessAllowed);
	}
}