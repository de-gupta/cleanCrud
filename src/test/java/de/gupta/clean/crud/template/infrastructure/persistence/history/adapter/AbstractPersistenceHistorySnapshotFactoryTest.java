package de.gupta.clean.crud.template.infrastructure.persistence.history.adapter;

import de.gupta.clean.crud.template.infrastructure.persistence.history.model.AuditActor;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertSame;

class AbstractPersistenceHistorySnapshotFactoryTest
{
	@Test
	void createSnapshotDelegatesToSnapshotOf()
	{
		TestPersistenceModel model = new TestPersistenceModel("id");
		TestHistoryModel historyModel = new TestHistoryModel("id");
		RecordingSnapshotFactory factory = new RecordingSnapshotFactory(historyModel);
		Instant decisionTime = Instant.now();
		Instant validFrom = decisionTime.minusSeconds(5);
		Instant validTo = decisionTime.plusSeconds(5);

		TestHistoryModel createdHistory = factory.createSnapshot(
				model, TemporalChangeType.UPDATED, decisionTime, validFrom, validTo);

		assertSame(model, factory.model);
		assertSame(historyModel, createdHistory);
		assertSame(historyModel, factory.returnedHistory);
	}

	private static final class RecordingSnapshotFactory
			extends AbstractPersistenceHistorySnapshotFactory<String, TestPersistenceModel, TestHistoryModel>
	{
		private final TestHistoryModel returnedHistory;
		private TestPersistenceModel model;

		@Override
		protected TestHistoryModel snapshotOf(
				final TestPersistenceModel model,
				final TemporalChangeType changeType,
				final Instant decisionTime,
				final Instant validFrom,
				final Instant validTo)
		{
			this.model = model;
			return returnedHistory;
		}

		private RecordingSnapshotFactory(final TestHistoryModel returnedHistory)
		{
			this.returnedHistory = returnedHistory;
		}
	}

	private record TestPersistenceModel(String id) implements WithID<String>
	{
	}

	private static final class TestHistoryModel implements TriTemporalHistoryModel<String>
	{
		private final String entityID;
		private AuditActor auditActor;
		private Instant transactionTime;
		private Instant validTo;

		@Override
		public String entityID()
		{
			return entityID;
		}

		@Override
		public AuditActor auditActor()
		{
			return auditActor;
		}

		@Override
		public void setAuditActor(final AuditActor auditActor)
		{
			this.auditActor = auditActor;
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
			return Instant.now();
		}

		@Override
		public Instant validFrom()
		{
			return Instant.now();
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