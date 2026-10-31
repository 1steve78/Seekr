package com.searchengine.dto;

import java.util.List;

public record SearchResponseDto(
    String query,
    long totalHits,
    long queryTimeMs,
    int page,
    int pageSize,
    List<SearchResultItem> results
) {
    public record SearchResultItem(
        String id,
        String url,
        String title,
        String snippet,
        String crawlDate
    ) {}
}