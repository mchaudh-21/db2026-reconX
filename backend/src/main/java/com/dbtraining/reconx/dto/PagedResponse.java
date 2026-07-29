package com.dbtraining.reconx.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * TICKET-ADV053 — stable, JSON-friendly pagination envelope.
 */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PagedResponse {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }

    public static <E, T> PagedResponse<T> of(
            Page<E> page,
            Function<? super E, T> mapper
    ) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(mapper, "mapper");

        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Compatibility alias for existing Day-5 starter comments/code.
     */
    public static <E, T> PagedResponse<T> from(
            Page<E> page,
            Function<? super E, T> mapper
    ) {
        return of(page, mapper);
    }
}
