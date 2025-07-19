package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.service;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractLongDomainIDGenerator implements DomainIDGenerator<Long>
{
	private final AtomicLong lastGeneratedID = new AtomicLong(0);

	@Override
	public Long generate()
	{
		long base = System.currentTimeMillis() * 1000L;
		return lastGeneratedID.updateAndGet(previous -> Math.max(previous + 1, base));
	}

	@Override
	public Long generate(final Long domainID)
	{
		return domainID + 1;
	}
}