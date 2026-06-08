package de.gupta.clean.crud.template.useCases.process.application.dispatch;

import de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationAction;

import java.util.Collection;

public interface ApplicationActionDispatcher
{
	void dispatch(Collection<? extends ApplicationAction> applicationActions);

	default void dispatch(final ApplicationAction applicationAction)
	{
		dispatch(java.util.List.of(applicationAction));
	}
}
