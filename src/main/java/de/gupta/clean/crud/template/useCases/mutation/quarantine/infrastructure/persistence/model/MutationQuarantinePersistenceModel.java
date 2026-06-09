package de.gupta.clean.crud.template.useCases.mutation.quarantine.infrastructure.persistence.model;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationFamily;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "mutation_quarantine")
public class MutationQuarantinePersistenceModel
{
	@Id
	@Column(name = "quarantine_id", nullable = false, updatable = false, length = 128)
	private String quarantineId;

	@Column(name = "aggregate_type", nullable = false, length = 512)
	private String aggregateType;

	@Column(name = "domain_id_type", nullable = false, length = 512)
	private String domainIdType;

	@Lob
	@Column(name = "domain_id_json", nullable = false)
	private String domainIdJson;

	@Column(name = "payload_type", nullable = false, length = 512)
	private String payloadType;

	@Lob
	@Column(name = "payload_json", nullable = false)
	private String payloadJson;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 64)
	private MutationSource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "family", nullable = false, length = 64)
	private MutationFamily family;

	@Column(name = "correlation_id", length = 255)
	private String correlationId;

	@Column(name = "causation_id", length = 255)
	private String causationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 64)
	private MutationQuarantineStatus status;

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

	public String aggregateType()
	{
		return aggregateType;
	}

	public void setAggregateType(final String aggregateType)
	{
		this.aggregateType = aggregateType;
	}

	public String domainIdType()
	{
		return domainIdType;
	}

	public void setDomainIdType(final String domainIdType)
	{
		this.domainIdType = domainIdType;
	}

	public String domainIdJson()
	{
		return domainIdJson;
	}

	public void setDomainIdJson(final String domainIdJson)
	{
		this.domainIdJson = domainIdJson;
	}

	public String payloadType()
	{
		return payloadType;
	}

	public void setPayloadType(final String payloadType)
	{
		this.payloadType = payloadType;
	}

	public String payloadJson()
	{
		return payloadJson;
	}

	public void setPayloadJson(final String payloadJson)
	{
		this.payloadJson = payloadJson;
	}

	public MutationSource source()
	{
		return source;
	}

	public void setSource(final MutationSource source)
	{
		this.source = source;
	}

	public MutationFamily family()
	{
		return family;
	}

	public void setFamily(final MutationFamily family)
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

	public MutationQuarantineStatus status()
	{
		return status;
	}

	public void setStatus(final MutationQuarantineStatus status)
	{
		this.status = status;
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
