package com.dbtraining.reconx.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * TICKET-ADV053 — Generic pagination wrapper.
 */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <E, T> PagedResponse<T> of(
            Page<E> page,
            Function<E, T> mapper
    ) {
        return new PagedResponse<>(
                page.getContent().stream()
                        .map(mapper)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
