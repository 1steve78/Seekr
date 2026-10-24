# Search Engine Monorepo

Phase 0 foundation for a modular search engine. The repository contains:

- `common` — shared crawl and index document contracts
- `crawler-service`
- `indexer-service`
- `search-api`
- `nlp-pipeline`

## Prerequisites

- Java 21
- Docker Engine and Docker Compose

## Start the complete Phase 0 stack

From the project root, build and start Solr, Kafka, Postgres, Redis, and all four
Spring Boot services:

```bash
docker compose up --build -d
docker compose ps
```

Compose waits for infrastructure health checks before starting dependent
services. Each Spring service exposes an Actuator health endpoint and Prometheus
metrics:

| Service | Local URL |
| --- | --- |
| Crawler | http://localhost:8081/actuator/health |
| Indexer | http://localhost:8082/actuator/health |
| Search API | http://localhost:8083/actuator/health |
| NLP pipeline | http://localhost:8084/actuator/health |
| Prometheus metrics | append `/actuator/prometheus` to a service URL |
| Solr documents core | http://localhost:8983/solr/documents/ |

Stop the stack with `docker compose down`. Postgres and Solr data are persisted
in Docker volumes; Kafka uses container-local storage and its data is discarded
when the Kafka container is recreated.

## Profiles and logging

Spring Boot services support `dev` and `prod` profiles through
`SPRING_PROFILES_ACTIVE`. Docker Compose uses `dev`. Console logs use structured
Logstash JSON output. Actuator exposes health, info, metrics, and Prometheus
scrape endpoints; health details are visible only in the `dev` profile.

## Build and test

Build all modules and run tests:

```bash
./mvnw -B verify
```

The Solr integration test uses Testcontainers and is skipped automatically when
Docker is unavailable. GitHub Actions runs the same verification on pushes and
pull requests.
