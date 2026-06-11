package de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence.model;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineStatus;
import jakarta.persistence.*;

import java.time.Instant;

@MappedSuperclass
public abstract class QuarantinePersistenceModel
{
	@Id
	@Column(name = "quarantine_id", nullable = false, updatable = false, length = 128)
	private String quarantineId;

	@Column(name = "aggregate_key", nullable = false, length = 512)
	private String aggregateKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 64)
	private OperationSource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "family", nullable = false, length = 64)
	private OperationFamily family;

	@Column(name = "correlation_id", length = 255)
	private String correlationId;

	@Column(name = "causation_id", length = 255)
	private String causationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 64)
	private QuarantineStatus status;

	@Lob
	@Column(name = "replay_inputs_json", nullable = false)
	private String replayInputsJson;

	@Lob
	@Column(name = "violations_json", nullable = false)
	private String violationsJson;

	@Column(name = "quarantined_at", nullable = false)
	private Instant quarantinedAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "replay_attempt_count", nullable = false)
	private int replayAttemptCount;

	@Column(name = "last_replay_at")
	private Instant lastReplayAt;

	@Column(name = "last_replay_outcome", length = 255)
	private String lastReplayOutcome;

	@Column(name = "last_replay_summary", length = 2000)
	private String lastReplaySummary;

	public String quarantineId()
	{
		return quarantineId;
	}

	public void setQuarantineId(final String quarantineId)
	{
		this.quarantineId = quarantineId;
	}

	public String aggregateKey()
	{
		return aggregateKey;
	}

	public void setAggregateKey(final String aggregateKey)
	{
		this.aggregateKey = aggregateKey;
	}

	public OperationSource source()
	{
		return source;
	}

	public void setSource(final OperationSource source)
	{
		this.source = source;
	}

	public OperationFamily family()
	{
		return family;
	}

	public void setFamily(final OperationFamily family)
	{
		this.family = family;
	}

	public String correlationId()
	{
		return correlationId;
	}

	public void setCorrelationId(final String correlationId)
	{
		this.correlationId = correlationId;
	}

	public String causationId()
	{
		return causationId;
	}

	public void setCausationId(final String causationId)
	{
		this.causationId = causationId;
	}

	public QuarantineStatus status()
	{
		return status;
	}

	public void setStatus(final QuarantineStatus status)
	{
		this.status = status;
	}

	public String replayInputsJson()
	{
		return replayInputsJson;
	}

	public void setReplayInputsJson(final String replayInputsJson)
	{
		this.replayInputsJson = replayInputsJson;
	}

	public String violationsJson()
	{
		return violationsJson;
	}

	public void setViolationsJson(final String violationsJson)
	{
		this.violationsJson = violationsJson;
	}

	public Instant quarantinedAt()
	{
		return quarantinedAt;
	}

	public void setQuarantinedAt(final Instant quarantinedAt)
	{
		this.quarantinedAt = quarantinedAt;
	}

	public Instant updatedAt()
	{
		return updatedAt;
	}

	public void setUpdatedAt(final Instant updatedAt)
	{
		this.updatedAt = updatedAt;
	}

	public int replayAttemptCount()
	{
		return replayAttemptCount;
	}

	public void setReplayAttemptCount(final int replayAttemptCount)
	{
		this.replayAttemptCount = replayAttemptCount;
	}

	public Instant lastReplayAt()
	{
		return lastReplayAt;
	}

	public void setLastReplayAt(final Instant lastReplayAt)
	{
		this.lastReplayAt = lastReplayAt;
	}

	public String lastReplayOutcome()
	{
		return lastReplayOutcome;
	}

	public void setLastReplayOutcome(final String lastReplayOutcome)
	{
		this.lastReplayOutcome = lastReplayOutcome;
	}

	public String lastReplaySummary()
	{
		return lastReplaySummary;
	}

	public void setLastReplaySummary(final String lastReplaySummary)
	{
		this.lastReplaySummary = lastReplaySummary;
	}
}