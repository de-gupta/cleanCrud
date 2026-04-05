package de.gupta.clean.crud.template.infrastructure.persistence.history.model;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.RequiredFieldNotSetException;
import de.gupta.clean.crud.template.domain.model.validation.Validatable;
import de.gupta.commons.utility.string.StringSanitizationUtility;

public interface AuditActor extends Validatable
{
	String actorId();

	String displayName();

	AuditActorKind actorKind();

	AuthenticationKind authenticationKind();

	String tokenId();

	String sessionId();

	String issuer();

	String clientId();

	@Override
	default void validate()
	{
		Unfolding.beckon(actorId())
		         .discern(StringSanitizationUtility::isNotBlank, RequiredFieldNotSetException.forMessage(
						 "auditActor.actorId must be provided when an audit actor exists"));
	}
}