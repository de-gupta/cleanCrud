package de.gupta.clean.crud.template.useCases.crud.common.utility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class PageUtility
{
	public static <S, T> Page<T> mapPage(Page<? extends S> sourcePage, Function<S, T> mappingFunction)
	{
		List<T> mappedContent = sourcePage.getContent().stream()
										  .map(mappingFunction)
										  .toList();

		return new PageImpl<>(mappedContent, sourcePage.getPageable(), sourcePage.getTotalElements());
	}

	public static <S, T> Page<T> mapPageOptionallyAndFilter(Page<? extends S> sourcePage,
															Function<S, Optional<T>> mappingFunction)
	{
		List<T> mappedContent = sourcePage.getContent().stream()
										  .map(mappingFunction)
										  .flatMap(Optional::stream)
										  .toList();

		return new PageImpl<>(mappedContent, sourcePage.getPageable(), sourcePage.getTotalElements());
	}

	private PageUtility()
	{
	}
}