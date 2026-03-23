package de.gupta.clean.crud.template.infrastructure.persistence.transaction;

import java.util.function.Supplier;

public interface PersistenceTransactionRunner
{
	<T> T inTransaction(Supplier<T> action);

	default void inTransaction(final Runnable action)
	{
		inTransaction(() ->
		{
			action.run();
			return null;
		});
	}
}