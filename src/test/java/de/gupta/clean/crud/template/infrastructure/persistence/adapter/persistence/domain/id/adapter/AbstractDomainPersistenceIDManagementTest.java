package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter;

import de.gupta.clean.crud.template.domain.model.builder.BuilderFactories;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.AbstractDomainPersistenceAdapterHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.AbstractDomainPersistenceAdapterModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.repository.DomainPersistenceAdapterRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.service.DomainIDGenerator;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.service.AuditActorSupplier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractDomainPersistenceIDManagementTest
{
	@Test
	void updateCreatesMappingAndCreatedHistoryWhenCurrentMappingIsMissing()
	{
		InMemoryDomainPersistenceAdapterRepository mappingRepository = new InMemoryDomainPersistenceAdapterRepository();
		InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
		TestDomainPersistenceIDManagement management = new TestDomainPersistenceIDManagement(
				mappingRepository, historyRepository);

		management.update(7L, "p-1");

		assertEquals("p-1", mappingRepository.findByDomainID(7L).orElseThrow().persistenceID());
		assertEquals(1, historyRepository.savedHistory.size());
		assertEquals(TemporalChangeType.CREATED, historyRepository.savedHistory.getFirst().changeType());
		assertEquals(7L, historyRepository.savedHistory.getFirst().entityID());
	}

	@Test
	void updateClosesCurrentHistoryAndWritesUpdatedHistoryWhenCurrentMappingExists()
	{
		InMemoryDomainPersistenceAdapterRepository mappingRepository = new InMemoryDomainPersistenceAdapterRepository();
		InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
		TestDomainPersistenceIDManagement management = new TestDomainPersistenceIDManagement(
				mappingRepository, historyRepository);

		TestDomainPersistenceAdapterModel currentMapping = TestDomainPersistenceAdapterModel.builder()
		                                                                                    .withDomainID(7L)
		                                                                                    .withPersistenceID("p-1")
		                                                                                    .build();
		mappingRepository.save(currentMapping);
		TestDomainPersistenceAdapterHistoryModel currentHistory = TestDomainPersistenceAdapterHistoryModel.builder()
		                                                                                                  .withDomainID(
																												  7L)
		                                                                                                  .withPersistenceID(
																												  "p-1")
		                                                                                                  .withChangeType(
																												  TemporalChangeType.CREATED)
		                                                                                                  .withDecisionTime(
																												  Instant.now())
		                                                                                                  .withValidFrom(
																												  Instant.now())
		                                                                                                  .withValidTo(
																												  de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalValidity.defaultEndValidity())
		                                                                                                  .build();
		historyRepository.currentHistoryByEntityID.put(7L, currentHistory);

		management.update(7L, "p-2");

		assertEquals("p-2", mappingRepository.findByDomainID(7L).orElseThrow().persistenceID());
		assertEquals(2, historyRepository.savedHistory.size());
		assertEquals(TemporalChangeType.UPDATED, historyRepository.savedHistory.getLast().changeType());
		assertTrue(historyRepository.savedHistory.getFirst().validTo().isBefore(
				de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalValidity.defaultEndValidity()));
	}

	private static final class TestDomainPersistenceIDManagement
			extends AbstractDomainPersistenceIDManagement<Long, String, TestDomainPersistenceAdapterModel,
			TestDomainPersistenceAdapterHistoryModel>
	{
		private TestDomainPersistenceIDManagement(
				final DomainPersistenceAdapterRepository<Long, String, TestDomainPersistenceAdapterModel> repository,
				final TriTemporalHistoryRepository<Long, TestDomainPersistenceAdapterHistoryModel> historyRepository)
		{
			super(repository,
					BuilderFactories.of(TestDomainPersistenceAdapterModel::builder),
					historyRepository,
					BuilderFactories.of(TestDomainPersistenceAdapterHistoryModel::builder),
					new FixedDomainIDGenerator(), AuditActorSupplier.none());
		}
	}

	private static final class FixedDomainIDGenerator implements DomainIDGenerator<Long>
	{
		@Override
		public Long generate()
		{
			return 1L;
		}

		@Override
		public Long generate(final Long domainID)
		{
			return domainID + 1;
		}
	}

	private static final class InMemoryDomainPersistenceAdapterRepository
			implements DomainPersistenceAdapterRepository<Long, String, TestDomainPersistenceAdapterModel>
	{
		private final Map<Long, TestDomainPersistenceAdapterModel> byDomainID = new ConcurrentHashMap<>();

		@Override
		public boolean existsByDomainID(final Long domainID)
		{
			return byDomainID.containsKey(domainID);
		}

		@Override
		public Collection<Long> existingDomainIDsFrom(final Collection<Long> domainIDs)
		{
			return domainIDs.stream().filter(byDomainID::containsKey).toList();
		}

		@Override
		public TestDomainPersistenceAdapterModel save(final TestDomainPersistenceAdapterModel model)
		{
			byDomainID.put(model.domainID(), model);
			return model;
		}

		@Override
		public void saveAll(final Collection<TestDomainPersistenceAdapterModel> models)
		{
			models.forEach(this::save);
		}

		@Override
		public void delete(final TestDomainPersistenceAdapterModel model)
		{

		}

		@Override
		public void deleteAll(final Collection<TestDomainPersistenceAdapterModel> testDomainPersistenceAdapterModels)
		{

		}

		@Override
		public Optional<TestDomainPersistenceAdapterModel> findByDomainID(final Long domainID)
		{
			return Optional.ofNullable(byDomainID.get(domainID));
		}

		@Override
		public Optional<TestDomainPersistenceAdapterModel> findByPersistenceID(final String persistenceID)
		{
			return byDomainID.values().stream().filter(model -> persistenceID.equals(model.persistenceID()))
			                 .findFirst();
		}

		@Override
		public Collection<TestDomainPersistenceAdapterModel> findByDomainIDs(final Collection<Long> domainIDs)
		{
			return List.of();
		}
	}

	private static final class InMemoryHistoryRepository
			implements TriTemporalHistoryRepository<Long, TestDomainPersistenceAdapterHistoryModel>
	{
		private final LinkedList<TestDomainPersistenceAdapterHistoryModel> savedHistory = new LinkedList<>();
		private final Map<Long, TestDomainPersistenceAdapterHistoryModel> currentHistoryByEntityID =
				new ConcurrentHashMap<>();

		@Override
		public TestDomainPersistenceAdapterHistoryModel save(final TestDomainPersistenceAdapterHistoryModel model)
		{
			savedHistory.add(model);
			currentHistoryByEntityID.put(model.entityID(), model);
			return model;
		}

		@Override
		public void saveAll(final Collection<TestDomainPersistenceAdapterHistoryModel> models)
		{
			models.forEach(this::save);
		}

		@Override
		public Optional<TestDomainPersistenceAdapterHistoryModel> findCurrentByEntityID(final Long entityID)
		{
			return Optional.ofNullable(currentHistoryByEntityID.get(entityID));
		}

		@Override
		public Collection<TestDomainPersistenceAdapterHistoryModel> findCurrentByEntityIDs(
				final Collection<Long> entityIDs)
		{
			return entityIDs.stream()
			                .map(currentHistoryByEntityID::get)
			                .filter(Objects::nonNull)
			                .toList();
		}
	}

	private static final class TestDomainPersistenceAdapterModel
			extends AbstractDomainPersistenceAdapterModel<Long, String>
	{
		private static Builder builder()
		{
			return new Builder(new TestDomainPersistenceAdapterModel());
		}

		private static final class Builder
				extends
				AbstractDomainPersistenceAdapterModel.AbstractBuilder<Long, String, TestDomainPersistenceAdapterModel>
		{
			@Override
			protected TestDomainPersistenceAdapterModel doBuild()
			{
				return (TestDomainPersistenceAdapterModel) model;
			}

			private Builder(final AbstractDomainPersistenceAdapterModel<Long, String> model)
			{
				super(model);
			}
		}
	}

	private static final class TestDomainPersistenceAdapterHistoryModel
			extends AbstractDomainPersistenceAdapterHistoryModel<Long, String>
	{
		private static Builder builder()
		{
			return new Builder(new TestDomainPersistenceAdapterHistoryModel());
		}

		private static final class Builder
				extends
				AbstractDomainPersistenceAdapterHistoryModel.AbstractBuilder<Long, String, TestDomainPersistenceAdapterHistoryModel>
		{
			@Override
			protected TestDomainPersistenceAdapterHistoryModel doBuild()
			{
				return (TestDomainPersistenceAdapterHistoryModel) model;
			}

			private Builder(final AbstractDomainPersistenceAdapterHistoryModel<Long, String> model)
			{
				super(model);
			}
		}
	}
}