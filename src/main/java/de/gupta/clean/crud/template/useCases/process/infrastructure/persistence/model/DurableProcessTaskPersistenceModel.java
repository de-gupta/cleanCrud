package de.gupta.clean.crud.template.useCases.process.infrastructure.persistence.model;

import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "durable_process_task")
public class DurableProcessTaskPersistenceModel
{
	@Id
	@Column(name = "task_id", nullable = false, updatable = false, length = 128)
	private String taskId;

	@Column(name = "process_type", nullable = false, length = 255)
	private String processType;

	@Column(name = "correlation_id", nullable = false, length = 255)
	private String correlationId;

	@Column(name = "payload_type", nullable = false, length = 512)
	private String payloadType;

	@Lob
	@Column(name = "payload_json", nullable = false)
	private String payloadJson;

	@Column(name = "retry_max_attempts", nullable = false)
	private int retryMaxAttempts;

	@Column(name = "retry_initial_delay_millis", nullable = false)
	private long retryInitialDelayMillis;

	@Column(name = "retry_multiplier", nullable = false)
	private double retryMultiplier;

	@Column(name = "retry_max_delay_millis")
	private Long retryMaxDelayMillis;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 64)
	private DurableProcessTaskStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_attempt_at")
	private Instant nextAttemptAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "last_failure_summary", length = 2000)
	private String lastFailureSummary;

	@Column(name = "last_outcome_code", length = 255)
	private String lastOutcomeCode;

	public String taskId()
	{
		return taskId;
	}

	public void setTaskId(final String taskId)
	{
		this.taskId = taskId;
	}

	public String processType()
	{
		return processType;
	}

	public void setProcessType(final String processType)
	{
		this.processType = processType;
	}

	public String correlationId()
	{
		return correlationId;
	}

	public void setCorrelationId(final String correlationId)
	{
		this.correlationId = correlationId;
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

	public int retryMaxAttempts()
	{
		return retryMaxAttempts;
	}

	public void setRetryMaxAttempts(final int retryMaxAttempts)
	{
		this.retryMaxAttempts = retryMaxAttempts;
	}

	public long retryInitialDelayMillis()
	{
		return retryInitialDelayMillis;
	}

	public void setRetryInitialDelayMillis(final long retryInitialDelayMillis)
	{
		this.retryInitialDelayMillis = retryInitialDelayMillis;
	}

	public double retryMultiplier()
	{
		return retryMultiplier;
	}

	public void setRetryMultiplier(final double retryMultiplier)
	{
		this.retryMultiplier = retryMultiplier;
	}

	public Long retryMaxDelayMillis()
	{
		return retryMaxDelayMillis;
	}

	public void setRetryMaxDelayMillis(final Long retryMaxDelayMillis)
	{
		this.retryMaxDelayMillis = retryMaxDelayMillis;
	}

	public DurableProcessTaskStatus status()
	{
		return status;
	}

	public void setStatus(final DurableProcessTaskStatus status)
	{
		this.status = status;
	}

	public int attemptCount()
	{
		return attemptCount;
	}

	public void setAttemptCount(final int attemptCount)
	{
		this.attemptCount = attemptCount;
	}

	public Instant nextAttemptAt()
	{
		return nextAttemptAt;
	}

	public void setNextAttemptAt(final Instant nextAttemptAt)
	{
		this.nextAttemptAt = nextAttemptAt;
	}

	public Instant createdAt()
	{
		return createdAt;
	}

	public void setCreatedAt(final Instant createdAt)
	{
		this.createdAt = createdAt;
	}

	public Instant updatedAt()
	{
		return updatedAt;
	}

	public void setUpdatedAt(final Instant updatedAt)
	{
		this.updatedAt = updatedAt;
	}

	public String lastFailureSummary()
	{
		return lastFailureSummary;
	}

	public void setLastFailureSummary(final String lastFailureSummary)
	{
		this.lastFailureSummary = lastFailureSummary;
	}

	public String lastOutcomeCode()
	{
		return lastOutcomeCode;
	}

	public void setLastOutcomeCode(final String lastOutcomeCode)
	{
		this.lastOutcomeCode = lastOutcomeCode;
	}
}
