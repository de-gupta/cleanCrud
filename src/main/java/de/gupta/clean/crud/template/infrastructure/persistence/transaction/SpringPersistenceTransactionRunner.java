package de.gupta.clean.crud.template.infrastructure.persistence.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

public final class SpringPersistenceTransactionRunner implements PersistenceTransactionRunner
{
	private final TransactionOperations transactionOperations;

	public static SpringPersistenceTransactionRunner withTransactionManager(
			final PlatformTransactionManager transactionManager)
	{
		return new SpringPersistenceTransactionRunner(transactionManager);
	}

	@Override
	public <T> T inTransaction(final Supplier<T> action)
	{
		return transactionOperations.execute(_ -> action.get());
	}

	private SpringPersistenceTransactionRunner(final PlatformTransactionManager transactionManager)
	{
		this(new TransactionTemplate(transactionManager));
	}

	private SpringPersistenceTransactionRunner(final TransactionOperations transactionOperations)
	{
		this.transactionOperations = transactionOperations;
	}
}