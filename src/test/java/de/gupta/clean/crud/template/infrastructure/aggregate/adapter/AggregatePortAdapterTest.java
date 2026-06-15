package de.gupta.clean.crud.template.infrastructure.aggregate.adapter;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeletePersistenceService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchPersistenceService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.SavePersistenceService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.UpdatePersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregatePortAdapterTest
{
	@Test
	void aggregateMutationPortAdapterDelegatesToPersistenceServices()
	{
		TestMutationServices services = new TestMutationServices();
		AggregateMutationPortAdapter<String, String, String, String> adapter =
				AggregateMutationPortAdapter.withPersistenceServices(services, services, services);

		assertEquals("saved", adapter.create("saved").model());
		adapter.put("id", "put");
		assertEquals("update", adapter.update("id", "update").model());
		adapter.delete("id");

		assertEquals(List.of("saved"), services.saved);
		assertEquals(List.of("id:put"), services.puts);
		assertEquals(List.of("id:update"), services.updates);
		assertEquals(List.of("id"), services.deletes);
	}

	@Test
	void aggregateFetchPortAdapterDelegatesToFetchPersistenceService()
	{
		TestFetchService service = new TestFetchService();
		AggregateFetchPortAdapter<String, String> adapter =
				AggregateFetchPortAdapter.withPersistenceService(service);

		assertEquals("one", adapter.findById("one").orElseThrow().model());
		assertEquals(2, adapter.findByIds(Set.of("one", "two")).size());
		assertEquals(2, adapter.findAll().size());
		assertEquals(2, adapter.findAll(Pageable.unpaged()).getContent().size());
	}

	private static final class TestMutationServices
			implements SavePersistenceService<String, String>,
			UpdatePersistenceService<String, String>,
			DeletePersistenceService<String>
	{
		private final List<String> saved = new java.util.ArrayList<>();
		private final List<String> puts = new java.util.ArrayList<>();
		private final List<String> updates = new java.util.ArrayList<>();
		private final List<String> deletes = new java.util.ArrayList<>();

		@Override
		public IdentifiedModel<String, String> save(final String model)
		{
			saved.add(model);
			return IdentifiedModel.of("id", model);
		}

		@Override
		public Collection<IdentifiedModel<String, String>> saveAll(final Collection<String> models)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAtId(final String id, final String model)
		{
			puts.add(id + ":" + model);
		}

		@Override
		public IdentifiedModel<String, String> updateById(final String id, final String model)
		{
			updates.add(id + ":" + model);
			return IdentifiedModel.of(id, model);
		}

		@Override
		public Collection<IdentifiedModel<String, String>> updateAllById(
				final Collection<IdentifiedModel<String, String>> models)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void deleteById(final String id)
		{
			deletes.add(id);
		}

		@Override
		public void deleteAllById(final Collection<String> ids)
		{
			throw new UnsupportedOperationException();
		}
	}

	private static final class TestFetchService implements FetchPersistenceService<String, String>
	{
		@Override
		public Collection<IdentifiedModel<String, String>> findAll()
		{
			return List.of(IdentifiedModel.of("one", "one"), IdentifiedModel.of("two", "two"));
		}

		@Override
		public org.springframework.data.domain.Page<IdentifiedModel<String, String>> findAll(final Pageable pageable)
		{
			return new PageImpl<>(findAll().stream().toList());
		}

		@Override
		public Optional<IdentifiedModel<String, String>> findById(final String id)
		{
			return Optional.of(IdentifiedModel.of(id, id));
		}

		@Override
		public Collection<IdentifiedModel<String, String>> findByIds(final Set<String> IDs)
		{
			return IDs.stream().map(id -> IdentifiedModel.of(id, id)).toList();
		}
	}
}