package de.gupta.clean.crud.template.domain.relationship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RelationshipBuilderTest
{
	@Test
	void ownedDefaultsAreApplied()
	{
		var relationship = Relationship.owned("notes", NoteModel.class)
		                               .satelliteApiIdType(Long.class)
		                               .satelliteDomainIdType(Long.class)
		                               .satellitePersistenceIdType(java.util.UUID.class)
		                               .build();

		assertEquals(RelationshipKind.OWNED, relationship.relationshipKind());
		assertEquals(LifecycleSemantics.of(true, true, false, false, true), relationship.lifecycleSemantics());
	}

	@Test
	void referencedDefaultsAndOverridesAreApplied()
	{
		var relationship = Relationship.referenced("organisation", OrganisationModel.class)
		                               .satelliteApiIdType(Long.class)
		                               .satelliteDomainIdType(Long.class)
		                               .satellitePersistenceIdType(java.util.UUID.class)
		                               .reconciliationStrategy(ReconciliationStrategy.REPLACE)
		                               .cascadeUpdate(false)
		                               .build();

		assertEquals(RelationshipKind.REFERENCED, relationship.relationshipKind());
		assertEquals(LifecycleSemantics.of(false, false, false, false, true), relationship.lifecycleSemantics());
		assertEquals(ReconciliationStrategy.REPLACE, relationship.reconciliationStrategy());
	}

	private interface OrganisationModel
	{
	}

	private interface NoteModel
	{
	}
}