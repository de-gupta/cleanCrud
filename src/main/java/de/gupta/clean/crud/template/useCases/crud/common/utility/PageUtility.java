package de.gupta.clean.crud.template.useCases.crud.common.utility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PageUtility
{
	public static <S, T> Page<T> mapPage(Page<? extends S> sourcePage, Function<S, T> mappingFunction)
	{
		List<T> mappedContent = sourcePage.getContent().stream()
										  .map(mappingFunction)
										  .toList();

		return new PageImpl<>(mappedContent, sourcePage.getPageable(), sourcePage.getTotalElements());
	}

	public static <S> Page<S> filterPage(Page<S> sourcePage, Predicate<S> filter)
	{
		List<S> mappedContent = sourcePage.getContent().stream()
										  .filter(filter)
										  .toList();

		return new PageImpl<>(mappedContent, sourcePage.getPageable(), sourcePage.getTotalElements());
	}

	private PageUtility()
	{
	}
}