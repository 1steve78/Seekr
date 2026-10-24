package com.searchengine.crawler.politeness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RobotsTxtServiceTest {

    private MockWebServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws Exception {
        server.shutdown();
    }

    @Test
    void fetchesAndCachesRulesForTheOrigin() throws Exception {
        server.enqueue(new MockResponse().setBody("""
                User-agent: *
                Disallow: /private
                Allow: /private/public
                """));
        RobotsTxtService service = new RobotsTxtService(new OkHttpClient());
        String origin = server.url("/").toString();

        assertFalse(service.isAllowed(origin + "private/secret"));
        assertTrue(service.isAllowed(origin + "private/public/page"));
        assertTrue(service.isAllowed(origin + "public"));

        assertEquals(1, server.getRequestCount());
        RecordedRequest request = server.takeRequest();
        assertEquals("/robots.txt", request.getPath());
        assertEquals("SearchEngineBot/1.0", request.getHeader("User-Agent"));
    }
}
