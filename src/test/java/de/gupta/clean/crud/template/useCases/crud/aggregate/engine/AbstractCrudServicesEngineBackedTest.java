package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinitionContract;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.AbstractDeleteService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.AbstractFetchService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.AbstractSaveService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.AbstractUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractCrudServicesEngineBackedTest
{
	@Test
	void saveAndUpdateServicesMapDomainResultsToResponseModels()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		DefaultAggregateLifecycleEngine engine = new DefaultAggregateLifecycleEngine(new InlineTransactionRunner());

		TestSaveService saveService = new TestSaveService(definition, engine);
		TestUpdateService updateService = new TestUpdateService(definition, engine);

		assertEquals("response:saved", saveService.save("saved").model());
		definition.store.put("id", "before");
		assertEquals("response:patched", updateService.updateById("id", "patched").model());
	}

	@Test
	void fetchAndDeleteServicesPreserveDomainBehavior()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		definition.store.put("id", "value");
		DefaultAggregateLifecycleEngine engine = new DefaultAggregateLifecycleEngine(new InlineTransactionRunner());

		TestFetchService fetchService = new TestFetchService(definition, engine);
		TestDeleteService deleteService = new TestDeleteService(definition, engine);

		assertEquals("value", fetchService.findById("id").model());
		deleteService.deleteById("id");
		assertEquals(List.of("id"), definition.deletedIds);
	}

	private static final class TestSaveService
			extends AbstractSaveService<String, String, String, String, String>
	{
		private TestSaveService(
				final AggregateCrudDefinition<String, String, String, String, String> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class TestUpdateService
			extends AbstractUpdateService<String, String, String, String, String>
	{
		private TestUpdateService(
				final AggregateCrudDefinition<String, String, String, String, String> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class TestFetchService
			extends AbstractFetchService<String, String, String, String, String>
	{
		private TestFetchService(
				final AggregateCrudDefinition<String, String, String, String, String> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class TestDeleteService
			extends AbstractDeleteService<String, String, String, String, String>
	{
		private TestDeleteService(
				final AggregateCrudDefinition<String, String, String, String, String> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class TestAggregateDefinition
			implements AggregateCrudDefinition<String, String, String, String, String>
	{
		private final java.util.Map<String, String> store = new java.util.HashMap<>();
		private final java.util.List<String> deletedIds = new java.util.ArrayList<>();

		@Override
		public AggregateMutationPort<String, String, String, String> mutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, String> create(final String domainModel)
				{
					store.put("saved", domainModel);
					return IdentifiedModel.of("saved", domainModel);
				}

				@Override
				public void put(final String domainId, final String domainModel)
				{
					store.put(domainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, String> update(final String domainId, final String domainModel)
				{
					store.put(domainId, domainModel);
					return IdentifiedModel.of(domainId, domainModel);
				}

				@Override
				public void delete(final String domainId)
				{
					deletedIds.add(domainId);
				}
			};
		}

		@Override
		public AggregateFetchPort<String, String> fetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, String>> findById(final String domainId)
				{
					return Optional.ofNullable(store.get(domainId)).map(model -> IdentifiedModel.of(domainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, String>> findByIds(final Set<String> domainIds)
				{
					return domainIds.stream().flatMap(id -> findById(id).stream()).toList();
				}

				@Override
				public Collection<IdentifiedModel<String, String>> findAll()
				{
					return store.entrySet().stream().map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
					            .toList();
				}

				@Override
				public Slice<IdentifiedModel<String, String>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		@Override
		public DomainModelBuilder<String, String> createBuilder()
		{
			return value -> value;
		}

		@Override
		public DomainModelPatcher<String, String> patcher()
		{
			return (_, patch) -> patch;
		}

		@Override
		public DomainResponseBuilder<String, String> responseBuilder()
		{
			return value -> "response:" + value;
		}

		@Override
		public InsertionPolicy<String> insertionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public PatchPolicy<String> patchPolicy()
		{
			return (_, _) ->
			{
			};
		}

		@Override
		public DeletionPolicy<String> deletionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public DomainSecurityPolicy<String> securityPolicy()
		{
			return DomainSecurityPolicy.allowing();
		}

		@Override
		public DuplicateDefinition<String> duplicateDefinition()
		{
			return String::equals;
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, String, String, String>>
		relationshipDefinitions()
		{
			return List.of();
		}
	}

	private static final class InlineTransactionRunner implements PersistenceTransactionRunner
	{
		@Override
		public <T> T inTransaction(final Supplier<T> action)
		{
			return action.get();
		}
	}
}