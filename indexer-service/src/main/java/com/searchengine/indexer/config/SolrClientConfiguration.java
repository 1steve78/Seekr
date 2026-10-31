package com.searchengine.indexer.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrClientConfiguration {

    @Bean
    SolrClient solrClient(
            @Value("${solr.url}") String solrUrl,
            @Value("${solr.collection}") String collection) {
        return new HttpSolrClient.Builder(solrUrl + "/" + collection).build();
    }
}
