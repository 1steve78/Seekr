package com.searchengine.crawler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    @Test
    void normalizesHostPathQueryAndTrackingParameters() {
        assertEquals(
                "http://example.com/article?q=search",
                UrlNormalizer.normalize(
                        " HTTP://WWW.Example.com/article/?utm_source=newsletter&q=search&fbclid=click#top "));
    }

    @Test
    void sortsQueryParametersAndUsesRootForEmptyPath() {
        assertEquals(
                "https://example.com/?a=1&b=2",
                UrlNormalizer.normalize("https://example.com?b=2&a=1"));
    }

    @Test
    void returnsNullForMalformedUrls() {
        assertNull(UrlNormalizer.normalize("http://[invalid"));
    }

    @Test
    void hashesContentWithSha256() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                UrlNormalizer.sha256("abc"));
    }
}
