package de.gupta.clean.crud.template.useCases.query.property.api.web;

import de.gupta.commons.utility.comparison.ComparisonType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

public interface SpringRestPropertyQueryController<Property, APIModelResponse>
{
	@GetMapping("")
	ResponseEntity<Collection<APIModelResponse>> queryBy(
			@RequestParam(name = "propertyValue") final Property propertyValue,
			@RequestParam(name = "comparisonType", required = false) final ComparisonType comparisonType
	);
}