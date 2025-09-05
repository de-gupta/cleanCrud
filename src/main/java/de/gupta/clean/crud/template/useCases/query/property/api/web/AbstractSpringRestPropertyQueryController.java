package de.gupta.clean.crud.template.useCases.query.property.api.web;

import de.gupta.clean.crud.template.useCases.query.property.facade.PropertyQueryServiceFacade;
import de.gupta.commons.utility.comparison.ComparisonType;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;

import java.util.Collection;

public abstract class AbstractSpringRestPropertyQueryController<Property, APIModelResponse>
		implements SpringRestPropertyQueryController<Property, APIModelResponse>
{
	private final String propertyName;
	private final PropertyQueryServiceFacade<Property, APIModelResponse> service;

	@Override
	public ResponseEntity<Collection<APIModelResponse>> queryBy(final Property propertyValue,
																@NotNull(message = "Comparison type cannot be null") final ComparisonType comparisonType)
	{
		return ResponseEntity.ok(service.queryBy(propertyName, propertyValue, comparisonType));
	}

	protected AbstractSpringRestPropertyQueryController(
			final String propertyName, final PropertyQueryServiceFacade<Property, APIModelResponse> service)
	{
		this.propertyName = propertyName;
		this.service = service;
	}
}