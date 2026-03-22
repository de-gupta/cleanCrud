package de.gupta.clean.crud.template.useCases.crud.common.utility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

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

	public static <S, T> Slice<T> mapSlice(Slice<? extends S> sourceSlice, Function<S, T> mappingFunction)
	{
		List<T> mappedContent = sourceSlice.getContent().stream()
										   .map(mappingFunction)
										   .toList();

		return new SliceImpl<>(mappedContent, sourceSlice.getPageable(), sourceSlice.hasNext());
	}

	public static <S> Slice<S> filterSlice(Slice<S> sourceSlice, Predicate<S> filter)
	{
		List<S> mappedContent = sourceSlice.getContent().stream()
										   .filter(filter)
										   .toList();

		return new SliceImpl<>(mappedContent, sourceSlice.getPageable(), sourceSlice.hasNext());
	}

	private PageUtility()
	{
	}
}