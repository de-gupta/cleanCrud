package de.gupta.clean.crud.template.useCases.query.specification;

import de.gupta.commons.utility.comparison.ComparisonType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Describes a queryable property of an entity/model with typed metadata and
 * a strategy to build JPA predicates.
 *
 * @param <M> entity/model type
 * @param <P> property Java type
 */
public final class PropertyDescriptor<M, P>
{
	private final String name;
	private final Class<P> type;
	private final EnumSet<ComparisonType> allowedComparisons;
	private final Function<Root<M>, Path<P>> jpaPathResolver;
	private final JpaPredicateFactory<P> predicateFactory;

	public PropertyDescriptor(
			final String name,
			final Class<P> type,
			final Set<ComparisonType> allowedComparisons,
			final Function<Root<M>, Path<P>> jpaPathResolver,
			final JpaPredicateFactory<P> predicateFactory)
	{
		this.name = Objects.requireNonNull(name, "name");
		this.type = Objects.requireNonNull(type, "type");
		this.allowedComparisons = allowedComparisons == null || allowedComparisons.isEmpty()
				? EnumSet.allOf(ComparisonType.class)
				: EnumSet.copyOf(allowedComparisons);
		this.jpaPathResolver = Objects.requireNonNull(jpaPathResolver, "jpaPathResolver");
		this.predicateFactory = Objects.requireNonNull(predicateFactory, "predicateFactory");
	}

	public String name() { return name; }
	public Class<P> type() { return type; }
	public Set<ComparisonType> allowedComparisons() { return Collections.unmodifiableSet(allowedComparisons); }
	public Function<Root<M>, Path<P>> jpaPathResolver() { return jpaPathResolver; }
	public JpaPredicateFactory<P> predicateFactory() { return predicateFactory; }

	public boolean supports(final ComparisonType comparisonType)
	{
		return allowedComparisons.contains(comparisonType);
	}

	public Predicate buildPredicate(final Root<M> root, final CriteriaBuilder cb, final P value,
									 final ComparisonType comparisonType)
	{
		if (!supports(comparisonType))
			throw new IllegalArgumentException("Comparison " + comparisonType + " not allowed for property " + name);
		final Path<P> path = jpaPathResolver.apply(root);
		return predicateFactory.build(path, cb, value, comparisonType);
	}
}