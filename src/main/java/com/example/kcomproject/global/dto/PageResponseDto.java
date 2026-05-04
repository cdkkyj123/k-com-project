package com.example.kcomproject.global.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

/* 공통 응답 WrapperDTO
 Page객체를 그대로 노출하지 않고, 필요한 부분만 공통응답 포맷으로 변환해주는 DTO*/
@Builder
public record PageResponseDto<T> (
        // 실제 데이터 리스트
        List<T> content,

        // 현재 페이지 (Cursor 기반에서는 null 가능)
        Integer pageNumber,

        // 한 페이지에 몇개씩 보여줄지
        int size,

        // 전체 데이터 개수 (Cursor 기반에서는 null 가능)
        Long totalElements,

        // 전체 페이지 수 (Cursor 기반에서는 null 가능)
        Integer totalPages,

        // 다음 페이지 존재 여부 (Cursor 기반 필수)
        boolean hasNext,

        // 마지막 데이터의 ID (Cursor 기반 필수)
        Long lastId
) {
    public static <T, P> PageResponseDto<T> of(Page<P> page, List<T> content) {
        return PageResponseDto.<T>builder()
                .content(content)
                .pageNumber(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .lastId(null)
                .build();
    }

    public static <T> PageResponseDto<T> of(Page<T> page) {
        return PageResponseDto.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .lastId(null)
                .build();
    }

    public static <T> PageResponseDto<T> ofCursor(List<T> content, int size, boolean hasNext, Long lastId) {
        return PageResponseDto.<T>builder()
                .content(content)
                .size(size)
                .hasNext(hasNext)
                .lastId(lastId)
                .build();
    }
}
