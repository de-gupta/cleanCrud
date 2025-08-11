package de.gupta.clean.crud.template.useCases.query.specification;

import de.gupta.commons.utility.comparison.ComparisonType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Builds a JPA {@link Predicate} for a given property path, comparison type and value.
 *
 * @param <P> the Java type of the property being compared
 */
@FunctionalInterface
public interface JpaPredicateFactory<P>
{
	/**
	 * Build a predicate for the provided path and comparison.
	 *
	 * Implementations should only accept supported {@link ComparisonType}s for the property type
	 * and throw an {@link IllegalArgumentException} for unsupported combinations.
	 */
	Predicate build(Path<P> path, CriteriaBuilder cb, P value, ComparisonType comparisonType);
}