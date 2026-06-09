package de.gupta.clean.crud.template.useCases.process.domain.model.outcome;

public enum FailureClassification
{
	BUSINESS_REJECTION,
	TRANSIENT_TECHNICAL_FAILURE,
	PERMANENT_TECHNICAL_FAILURE;

	public boolean isRetryable()
	{
		return this == TRANSIENT_TECHNICAL_FAILURE;
	}

	public boolean isTechnical()
	{
		return this != BUSINESS_REJECTION;
	}
}
