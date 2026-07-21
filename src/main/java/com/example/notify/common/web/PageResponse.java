package com.example.notify.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

/** A stable, simplified pagination envelope so API responses don't leak Spring Data's Page internals. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
