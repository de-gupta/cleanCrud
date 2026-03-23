package de.gupta.clean.crud.template.domain.model.builder;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class BuilderFactoriesTest
{
	@Test
	void ofCreatesModelBuilderFactoryBackedBySupplier()
	{
		AtomicInteger createdBuilders = new AtomicInteger();
		ModelBuilderFactory<String, CountingBuilder> factory = BuilderFactories.of(() ->
		{
			createdBuilders.incrementAndGet();
			return new CountingBuilder();
		});

		CountingBuilder first = factory.builder();
		CountingBuilder second = factory.builder();

		assertEquals(2, createdBuilders.get());
		assertNotSame(first, second);
	}

	private static final class CountingBuilder implements ModelBuilder<String>
	{
		@Override
		public String build()
		{
			return "value";
		}
	}
}
