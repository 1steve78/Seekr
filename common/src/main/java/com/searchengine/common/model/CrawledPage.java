package com.searchengine.common.model;

import java.time.Instant;

public record CrawledPage(
        String url,
        Instant fetchedAt,
        int status,
        String rawHtmlPath,
        String contentHash) {
}
