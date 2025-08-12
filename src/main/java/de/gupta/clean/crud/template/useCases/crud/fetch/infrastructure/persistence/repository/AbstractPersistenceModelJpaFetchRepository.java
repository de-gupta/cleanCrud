package de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.repository.specification.PersistenceRepositorySpecificationJpaCriteriaAdapter;
import de.gupta.clean.crud.template.specification.ModelSpecification;
import de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.service.FetchPersistenceModelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractPersistenceModelJpaFetchRepository<PersistenceModel, PersistenceID, ConcretePersistenceModel extends PersistenceModel>
		implements FetchPersistenceModelRepository<PersistenceModel, PersistenceID>
{
	private final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository;
	private final JpaSpecificationExecutor<ConcretePersistenceModel> jpaSpecificationExecutor;
	private final PersistenceRepositorySpecificationJpaCriteriaAdapter<PersistenceModel, ConcretePersistenceModel>
			specificationAdapter;

	@Override
	public Collection<PersistenceModel> findAll()
	{
		return jpaRepository.findAll().stream().map(this::castUp).toList();
	}

	@Override
	public Collection<PersistenceModel> findAll(final ModelSpecification<PersistenceModel> filter)
	{
		return jpaSpecificationExecutor.findAll(
				specificationAdapter.toSpecification(filter)).stream().map(this::castUp).toList();
	}

	@Override
	public Page<PersistenceModel> findAll(final Pageable pageable)
	{
		return jpaRepository.findAll(pageable).map(this::castUp);
	}

	@Override
	public Page<PersistenceModel> findAll(final ModelSpecification<PersistenceModel> specification,
										  final Pageable pageable)
	{
		return jpaSpecificationExecutor.findAll(specificationAdapter.toSpecification(specification), pageable)
									   .map(this::castUp);
	}

	@Override
	public Optional<PersistenceModel> findById(final PersistenceID persistenceID)
	{
		return jpaRepository.findById(persistenceID).map(this::castUp);
	}

	@Override
	public Collection<PersistenceModel> findByIds(final Iterable<PersistenceID> IDs)
	{
		return jpaRepository.findAllById(IDs).stream().map(this::castUp).toList();
	}

	private PersistenceModel castUp(final ConcretePersistenceModel persistenceModel)
	{
		return persistenceModel;
	}

	protected AbstractPersistenceModelJpaFetchRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final JpaSpecificationExecutor<ConcretePersistenceModel> jpaSpecificationExecutor,
			final PersistenceRepositorySpecificationJpaCriteriaAdapter<PersistenceModel, ConcretePersistenceModel> specificationAdapter)
	{
		this.jpaRepository = jpaRepository;
		this.jpaSpecificationExecutor = jpaSpecificationExecutor;
		this.specificationAdapter = specificationAdapter;
	}
}