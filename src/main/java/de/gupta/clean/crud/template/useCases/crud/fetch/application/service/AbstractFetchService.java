package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.model.exceptions.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractFetchService<DomainModelResponse, DomainID, DomainModel>
		implements FetchService<DomainModelResponse, DomainID>
{
	private final FetchPersistenceService<DomainID, DomainModel> persistenceService;
	private final DomainResponseBuilder<DomainModel, DomainModelResponse> modelMapper;

	private final Predicate<DomainModel> accessAndDomainFilter;

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> findAll()
	{
		return persistenceService.findAll()
								 .stream()
								 .filter(convertFilter(accessAndDomainFilter))
								 .map(this::identifiedModel)
								 .toList();
	}

	@Override
	public Page<IdentifiedModel<DomainID, DomainModelResponse>> findAll(final Pageable pageable)
	{
		return PageUtility.mapPage(
				PageUtility.mapPageAndFilter(persistenceService.findAll(pageable), Function.identity(),
						convertFilter(accessAndDomainFilter)),
				this::identifiedModel);
	}

	@Override
	public IdentifiedModel<DomainID, DomainModelResponse> findById(final DomainID domainID)
	{
		return persistenceService.findById(domainID)
								 .filter(convertFilter(accessAndDomainFilter))
								 .map(this::identifiedModel)
								 .orElseThrow(() -> ResourceNotFoundException.withId(domainID));
	}

	@Override
	public Collection<IdentifiedModel<DomainID, DomainModelResponse>> findByIds(final Set<DomainID> IDs)
	{
		return persistenceService.findByIds(IDs).stream()
								 .filter(convertFilter(accessAndDomainFilter))
								 .map(this::identifiedModel).toList();
	}

	private Predicate<IdentifiedModel<DomainID, DomainModel>> convertFilter(final Predicate<DomainModel> filter)
	{
		return im -> filter.test(im.model());
	}

	private IdentifiedModel<DomainID, DomainModelResponse> identifiedModel(
			final IdentifiedModel<DomainID, DomainModel> domainModel)
	{
		return IdentifiedModel.of(domainModel.id(), modelMapper.toResponse(domainModel.model()));
	}

	protected AbstractFetchService(
			final FetchPersistenceService<DomainID, DomainModel> persistenceService,
			final DomainResponseBuilder<DomainModel, DomainModelResponse> modelMapper,
			final DomainSecurityPolicy<DomainModel> domainSecurityPolicy)
	{
		this(persistenceService, modelMapper, domainSecurityPolicy, Set.of());
	}

	protected AbstractFetchService(
			final FetchPersistenceService<DomainID, DomainModel> persistenceService,
			final DomainResponseBuilder<DomainModel, DomainModelResponse> modelMapper,
			final DomainSecurityPolicy<DomainModel> domainSecurityPolicy,
			final Set<Predicate<DomainModel>> additionalFilters)
	{
		this.persistenceService = persistenceService;
		this.modelMapper = modelMapper;
		this.accessAndDomainFilter =
				additionalFilters.stream().reduce(domainSecurityPolicy::isAccessAllowed, Predicate::and);
	}
}