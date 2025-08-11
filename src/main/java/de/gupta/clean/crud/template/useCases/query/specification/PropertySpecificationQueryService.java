package de.gupta.clean.crud.template.useCases.query.specification;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.commons.utility.comparison.ComparisonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PropertySpecificationQueryService<Property, DomainID, DomainModel, DomainModelResponse>
{
	Page<IdentifiedModel<DomainID, DomainModelResponse>> queryBy(
			final PropertyDescriptor<DomainModel, Property> property,
			final Property value,
			final ComparisonType comparisonType,
			final Pageable pageable);

	Slice<IdentifiedModel<DomainID, DomainModelResponse>> queryBySlice(
			final PropertyDescriptor<DomainModel, Property> property,
			final Property value,
			final ComparisonType comparisonType,
			final Pageable pageable);
}