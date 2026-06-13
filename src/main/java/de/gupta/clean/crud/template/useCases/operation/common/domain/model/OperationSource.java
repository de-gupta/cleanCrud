package de.gupta.clean.crud.template.useCases.operation.common.domain.model;

public enum OperationSource
{
	USER_INTENT,
	INTERNAL_COMMAND,
	AUTHORITATIVE_EXTERNAL_EVENT,
	PROCESS_EMITTED_ACTION,
	ADMINISTRATIVE_REPLAY
}
