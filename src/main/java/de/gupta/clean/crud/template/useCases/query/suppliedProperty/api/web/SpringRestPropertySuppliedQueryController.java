package de.gupta.clean.crud.template.useCases.query.suppliedProperty.api.web;

import de.gupta.commons.utility.comparison.ComparisonType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

public interface SpringRestPropertySuppliedQueryController<APIModelResponse>
{
	@GetMapping("")
	ResponseEntity<Collection<APIModelResponse>> queryBySuppliedProperty(
			@RequestParam(name = "comparisonType", required = false) final ComparisonType comparisonType
	);
}