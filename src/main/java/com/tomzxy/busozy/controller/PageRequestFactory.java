package com.tomzxy.busozy.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {

    public PageRequestFactory() {
    }

    public static PageRequest build(int page, int size, String sort, Sort.Direction defaultDirection) {
        int effectiveSize = Math.min(size, 100);
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : defaultDirection;
        return PageRequest.of(page, effectiveSize, Sort.by(direction, parts[0]));
    }
}
