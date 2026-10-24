package com.searchengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class SolrIntegrationTest {
    @Container
    private static final GenericContainer<?> solr = new GenericContainer<>(
            DockerImageName.parse("solr:9.7"))
            .withExposedPorts(8983)
            .withCommand("solr-precreate", "documents")
            .waitingFor(org.testcontainers.containers.wait.strategy.Wait
                    .forHttp("/solr/documents/admin/ping")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @Test
    void startsSolrWithTheDocumentsCoreHealthy() throws Exception {
        URI pingUri = URI.create("http://%s:%d/solr/documents/admin/ping"
                .formatted(solr.getHost(), solr.getMappedPort(8983)));
        HttpRequest request = HttpRequest.newBuilder(pingUri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"OK\""), response.body());
    }
}
