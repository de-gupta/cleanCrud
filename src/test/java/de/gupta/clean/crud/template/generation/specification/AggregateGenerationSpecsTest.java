package de.gupta.clean.crud.template.generation.specification;

import de.gupta.clean.crud.template.domain.relationship.Relationship;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AggregateGenerationSpecsTest
{
	@Test
	void buildsSpecificationWithOptionalIdsRelationshipsAndExtensionFlags()
	{
		var relationship = Relationship.referenced("manager", ManagerModel.class)
		                               .satelliteApiIdType(Long.class)
		                               .satelliteDomainIdType(Long.class)
		                               .satellitePersistenceIdType(java.util.UUID.class)
		                               .build();

		var specification = AggregateGenerationSpecs.aggregate(PersonModel.class)
		                                            .rootApiIdType(String.class)
		                                            .rootDomainIdType(Long.class)
		                                            .rootPersistenceIdType(java.util.UUID.class)
		                                            .relationship(relationship)
		                                            .postCommitOnSave()
		                                            .postCommitOnDelete()
		                                            .subprocessOnUpdate()
		                                            .build();

		assertEquals(PersonModel.class, specification.baseModelClass());
		assertEquals(String.class, specification.rootApiIdType());
		assertEquals(Long.class, specification.rootDomainIdType());
		assertEquals(java.util.UUID.class, specification.rootPersistenceIdType());
		assertEquals(relationship, specification.relationships().iterator().next());
		assertTrue(specification.postCommitHooks().save());
		assertFalse(specification.postCommitHooks().update());
		assertTrue(specification.postCommitHooks().delete());
		assertFalse(specification.subprocesses().save());
		assertTrue(specification.subprocesses().update());
		assertFalse(specification.subprocesses().delete());
	}

	@Test
	void defaultsExtensionFlagsToDisabled()
	{
		var specification = AggregateGenerationSpecs.aggregate(PersonModel.class).build();

		assertFalse(specification.postCommitHooks().anyEnabled());
		assertFalse(specification.subprocesses().anyEnabled());
	}

	interface PersonModel
	{
	}

	interface ManagerModel
	{
	}
}
