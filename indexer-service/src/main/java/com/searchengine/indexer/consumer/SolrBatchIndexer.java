package com.searchengine.indexer.consumer;

import com.searchengine.common.model.IndexedDoc;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SolrBatchIndexer {

    private static final Logger log = LoggerFactory.getLogger(SolrBatchIndexer.class);
    private final SolrClient solrClient;

    public SolrBatchIndexer(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    @KafkaListener(
        topics = "indexed-docs",
        groupId = "indexer-service-group",
        batch = "true",
        properties = {
            "max.poll.records=250",
            "fetch.min.bytes=65536",
            "fetch.max.wait.ms=1000"
        }
    )
    public void consumeBatch(List<IndexedDoc> docs) {
        if (docs.isEmpty()) return;

        List<SolrInputDocument> solrDocs = new ArrayList<>(docs.size());
        for (IndexedDoc doc : docs) {
            SolrInputDocument sDoc = new SolrInputDocument();
            sDoc.addField("id", doc.id());
            sDoc.addField("url", doc.url());
            sDoc.addField("title", doc.title());
            sDoc.addField("content", doc.content());
            sDoc.addField("lang", doc.lang());
            sDoc.addField("pagerank", doc.pagerank());
            sDoc.addField("crawlDate", Date.from(doc.crawlDate()));
            sDoc.addField("contentHash", doc.contentHash());
            solrDocs.add(sDoc);
        }

        try {
            solrClient.add(solrDocs);
            log.info("Indexed batch of {} documents into Solr", solrDocs.size());
        } catch (Exception e) {
            log.error("Failed to index batch of {} documents", solrDocs.size(), e);
            throw new RuntimeException("Solr batch write failed", e);
        }
    }
}
