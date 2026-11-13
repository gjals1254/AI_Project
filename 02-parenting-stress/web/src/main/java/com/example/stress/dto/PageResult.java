package com.example.stress.dto;

import java.util.List;

/** 화면용 페이지 정보 (page 는 0부터) */
public record PageResult<T>(List<T> content, int page, int size, long total) {

    public int totalPages() {
        return (int) Math.max(1, Math.ceil((double) total / size));
    }

    public boolean hasPrev() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }
}
