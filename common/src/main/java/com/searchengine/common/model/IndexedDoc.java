package com.searchengine.common.model;

import java.time.Instant;
import java.util.List;

public record IndexedDoc(
        String id,
        String url,
        String title,
        String content,
        String lang,
        List<String> entities,
        double pagerank,
        List<Float> vector,
        Instant crawlDate,
        String contentHash) {
}
