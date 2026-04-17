package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

/**
 * Defines how the runtime reconciles a requested satellite collection against the currently linked collection.
 * <p>
 * Supported strategies:
 * <ul>
 *     <li>{@link #REPLACE}: the requested collection becomes the full final linked collection</li>
 *     <li>{@link #MERGE_BY_ID}: requested mutations are applied to the current collection by explicit satellite
 *     domain ID</li>
 * </ul>
 * Business-key reconciliation is intentionally unsupported.
 */
public enum ReconciliationStrategy
{
	REPLACE,
	MERGE_BY_ID
}
