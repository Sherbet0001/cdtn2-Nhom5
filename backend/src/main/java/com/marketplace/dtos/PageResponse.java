package com.marketplace.dtos;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Định dạng dữ liệu phân trang chuẩn theo mục 3: { content, page, size, totalElements, totalPages }.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <T> PageResponse<T> fromPage(Page<?> pageData, List<T> mappedContent) {
        return PageResponse.<T>builder()
                .content(mappedContent)
                .page(pageData.getNumber())
                .size(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .build();
    }
}

