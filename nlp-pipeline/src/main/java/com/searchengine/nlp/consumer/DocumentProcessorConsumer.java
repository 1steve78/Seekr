package com.searchengine.nlp.consumer;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.searchengine.common.model.CrawledPage;
import com.searchengine.common.model.IndexedDoc;
import com.searchengine.nlp.extractor.BoilerplateExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;

@Service
public class DocumentProcessorConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessorConsumer.class);

    private final BoilerplateExtractor extractor;
    private final KafkaTemplate<String, IndexedDoc> kafkaTemplate;
    private final LanguageDetector languageDetector;

    public DocumentProcessorConsumer(BoilerplateExtractor extractor,
                                     KafkaTemplate<String, IndexedDoc> kafkaTemplate) {
        this.extractor = extractor;
        this.kafkaTemplate = kafkaTemplate;
        this.languageDetector = LanguageDetectorBuilder.fromLanguages(
            Language.ENGLISH, Language.SPANISH, Language.FRENCH, Language.GERMAN, Language.HINDI
        ).build();
    }

    @KafkaListener(topics = "raw-pages", groupId = "nlp-pipeline-group", concurrency = "4")
    public void processPage(CrawledPage page) {
        try {
            Path htmlPath = Path.of(page.rawHtmlPath());
            if (!Files.exists(htmlPath)) {
                log.warn("Raw HTML file does not exist for crawled page {}", page.url());
                return;
            }

            String rawHtml = Files.readString(htmlPath);
            var extracted = extractor.extract(rawHtml, page.url());

            if (extracted.body().length() < 120) return;

            Language lang = languageDetector.detectLanguageOf(extracted.body());
            if (lang != Language.ENGLISH) return;

            String docId = sha256(page.url());

            IndexedDoc indexedDoc = new IndexedDoc(
                docId,
                page.url(),
                extracted.title().isEmpty() ? page.url() : extracted.title(),
                extracted.body(),
                "en",
                Collections.emptyList(),
                1.0,
                Collections.emptyList(),
                page.fetchedAt(),
                page.contentHash()
            );

            kafkaTemplate.send("indexed-docs", docId, indexedDoc);
        } catch (Exception e) {
            log.error("Failed to process crawled page {}", page.url(), e);
            throw new IllegalStateException("Failed to process crawled page " + page.url(), e);
        }
    }

    private String sha256(String input) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
            .digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}