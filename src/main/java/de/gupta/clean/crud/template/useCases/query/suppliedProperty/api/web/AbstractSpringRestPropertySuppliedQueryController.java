package de.gupta.clean.crud.template.useCases.query.suppliedProperty.api.web;

import de.gupta.clean.crud.template.useCases.query.suppliedProperty.facade.PropertySuppliedQueryServiceFacade;
import de.gupta.commons.utility.comparison.ComparisonType;
import org.springframework.http.ResponseEntity;

import java.util.Collection;

public abstract class AbstractSpringRestPropertySuppliedQueryController<APIModelResponse>
		implements SpringRestPropertySuppliedQueryController<APIModelResponse>
{
	private final String propertyName;
	private final PropertySuppliedQueryServiceFacade<APIModelResponse> service;

	@Override
	public ResponseEntity<Collection<APIModelResponse>> queryBySuppliedProperty(final ComparisonType comparisonType)
	{
		return ResponseEntity.ok(service.queryBySuppliedProperty(propertyName, comparisonType));
	}

	protected AbstractSpringRestPropertySuppliedQueryController(
			final String propertyName, final PropertySuppliedQueryServiceFacade<APIModelResponse> service)
	{
		this.propertyName = propertyName;
		this.service = service;
	}
}