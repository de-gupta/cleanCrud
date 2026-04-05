package de.gupta.clean.crud.template.infrastructure.persistence.history.model;

import de.gupta.aletheia.functional.Unfolding;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(TriTemporalHistoryModelEntryListener.class)
public abstract class AbstractTriTemporalHistoryModel<EntityID> implements TriTemporalHistoryModel<EntityID>
{
	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, name = "entity_id")
	private EntityID entityID;

	@Column(nullable = false, name = "change_type")
	@Enumerated(EnumType.STRING)
	private TemporalChangeType changeType;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "actorId", column = @Column(name = "actor_id")),
			@AttributeOverride(name = "displayName", column = @Column(name = "actor_display_name")),
			@AttributeOverride(name = "actorKind", column = @Column(name = "actor_kind")),
			@AttributeOverride(name = "authenticationKind", column = @Column(name = "authentication_kind")),
			@AttributeOverride(name = "tokenId", column = @Column(name = "actor_token_id")),
			@AttributeOverride(name = "sessionId", column = @Column(name = "actor_session_id")),
			@AttributeOverride(name = "issuer", column = @Column(name = "actor_issuer")),
			@AttributeOverride(name = "clientId", column = @Column(name = "actor_client_id"))
	})
	private PersistedAuditActor persistedAuditActor;

	@Column(nullable = false, updatable = false, name = "transaction_time")
	private Instant transactionTime;

	@Column(nullable = false, name = "decision_time")
	private Instant decisionTime;

	@Column(nullable = false, name = "valid_from")
	private Instant validFrom;

	@Column(nullable = false, name = "valid_to")
	@Check(constraints = "valid_to >= valid_from")
	private Instant validTo;

	@Override
	public EntityID entityID()
	{
		return entityID;
	}

	@Override
	public AuditActor auditActor()
	{
		return persistedAuditActor;
	}

	@Override
	public void setAuditActor(final AuditActor auditActor)
	{
		applyAuditActor(auditActor);
		validate();
	}

	@Override
	public TemporalChangeType changeType()
	{
		return changeType;
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
		validate();
	}

	@Override
	public void validate()
	{
		Unfolding.beckon(persistedAuditActor)
		         .unlace(AuditActor::validate);
	}

	protected void applyAuditActor(final AuditActor auditActor)
	{
		this.persistedAuditActor = PersistedAuditActor.from(auditActor);
	}

	protected void setEntityID(final EntityID entityID)
	{
		this.entityID = entityID;
	}

	protected void setChangeType(final TemporalChangeType changeType)
	{
		this.changeType = changeType;
	}

	protected void setDecisionTime(final Instant decisionTime)
	{
		this.decisionTime = decisionTime;
	}

	protected void setValidFrom(final Instant validFrom)
	{
		this.validFrom = validFrom;
	}

	protected AbstractTriTemporalHistoryModel()
	{
	}
}