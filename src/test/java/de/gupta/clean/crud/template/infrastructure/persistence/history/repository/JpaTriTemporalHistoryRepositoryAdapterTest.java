package de.gupta.clean.crud.template.infrastructure.persistence.history.repository;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceStateConflictException;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalValidity;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaTriTemporalHistoryRepositoryAdapterTest
{
	@Test
	void findCurrentByEntityIDsThrowsWhenMultipleCurrentRecordsExistForOneEntity()
	{
		TriTemporalHistoryJpaRepository<String, TestHistoryModel> repository = repositoryReturning(List.of(
				new TestHistoryModel("entity-1"),
				new TestHistoryModel("entity-1")));
		JpaTriTemporalHistoryRepositoryAdapter<String, TestHistoryModel> adapter =
				new JpaTriTemporalHistoryRepositoryAdapter<>(repository);

		ResourceStateConflictException exception = assertThrows(ResourceStateConflictException.class,
				() -> adapter.findCurrentByEntityIDs(List.of("entity-1")));

		assertEquals("Multiple current history records found for entity IDs: [entity-1]", exception.getMessage());
	}

	@Test
	void findCurrentByEntityIDsReturnsUniqueCurrentRecords()
	{
		TestHistoryModel entityOneHistory = new TestHistoryModel("entity-1");
		TestHistoryModel entityTwoHistory = new TestHistoryModel("entity-2");
		TriTemporalHistoryJpaRepository<String, TestHistoryModel> repository = repositoryReturning(List.of(
				entityOneHistory,
				entityTwoHistory));
		JpaTriTemporalHistoryRepositoryAdapter<String, TestHistoryModel> adapter =
				new JpaTriTemporalHistoryRepositoryAdapter<>(repository);

		Collection<TestHistoryModel> histories = adapter.findCurrentByEntityIDs(List.of("entity-1", "entity-2"));

		assertEquals(List.of(entityOneHistory, entityTwoHistory), new ArrayList<>(histories));
	}

	@SuppressWarnings("unchecked")
	private static TriTemporalHistoryJpaRepository<String, TestHistoryModel> repositoryReturning(
			final Collection<TestHistoryModel> currentHistories)
	{
		return (TriTemporalHistoryJpaRepository<String, TestHistoryModel>) Proxy.newProxyInstance(
				TriTemporalHistoryJpaRepository.class.getClassLoader(),
				new Class[]{TriTemporalHistoryJpaRepository.class},
				(proxy, method, args) ->
				{
					if ("findAllByEntityIDInAndValidFromIsBeforeAndValidToIsAfter".equals(method.getName()))
					{
						Collection<String> entityIDs = (Collection<String>) args[0];
						return currentHistories.stream().filter(history -> entityIDs.contains(history.entityID()))
											   .toList();
					}
					if ("toString".equals(method.getName()))
					{
						return "InMemoryTriTemporalHistoryJpaRepository";
					}
					if ("hashCode".equals(method.getName()))
					{
						return System.identityHashCode(proxy);
					}
					if ("equals".equals(method.getName()))
					{
						return proxy == args[0];
					}
					throw new UnsupportedOperationException(method.getName());
				});
	}

	private static final class TestHistoryModel implements TriTemporalHistoryModel<String>
	{
		private final String entityID;
		private final Instant decisionTime = Instant.now();
		private final Instant validFrom = Instant.now();
		private Instant transactionTime = Instant.now();
		private Instant validTo = TemporalValidity.defaultEndValidity();

		@Override
		public String entityID()
		{
			return entityID;
		}

		@Override
		public TemporalChangeType changeType()
		{
			return TemporalChangeType.CREATED;
		}

		@Override
		public Instant transactionTime()
		{
			return transactionTime;
		}

		@Override
		public void setTransactionTime(final Instant transactionTime)
		{
			this.transactionTime = transactionTime;
		}

		@Override
		public Instant decisionTime()
		{
			return decisionTime;
		}

		@Override
		public Instant validFrom()
		{
			return validFrom;
		}

		@Override
		public Instant validTo()
		{
			return validTo;
		}

		@Override
		public void setValidTo(final Instant validTo)
		{
			this.validTo = validTo;
		}

		private TestHistoryModel(final String entityID)
		{
			this.entityID = entityID;
		}
	}
}