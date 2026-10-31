package com.searchengine.service;

import com.searchengine.dto.SearchResponseDto;
import com.searchengine.dto.SearchResponseDto.SearchResultItem;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service 
public class SearchService {
    
    private final SolrClient solrClient ;

    public SearchService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public SearchResponseDto search(String rawQuery , int page, int size) throws Exception{
        if(rawQuery == null || rawQuery.isBlank()){
            return new SearchResponseDto("",0,0,page,size,List.of());
            
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(rawQuery);

        solrQuery.set("defType", "edismax");
        solrQuery.set("qf", "title^3.0 content^1.0");
        solrQuery.set("pf", "title^5.0 content^2.0");
        solrQuery.set("ps", 2);
        solrQuery.set("mm", "2<-25%");
        solrQuery.setStart(page * size);
        solrQuery.setRows(size);
        solrQuery.setFields("id", "url", "title", "crawlDate");
        solrQuery.setHighlight(true);
        solrQuery.set("hl.method", "unified");
        solrQuery.addHighlightField("content");
        solrQuery.setHighlightSnippets(2);
        solrQuery.setHighlightFragsize(160);
        solrQuery.setHighlightSimplePre("<mark>");
        solrQuery.setHighlightSimplePost("</mark>");

        QueryResponse response = solrClient.query(solrQuery);
        Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();

        List<SearchResultItem> items = new ArrayList<>();
        for (SolrDocument doc : response.getResults()) {
            String id = (String) doc.getFieldValue("id");
            String url = (String) doc.getFieldValue("url");
            String title = firstOrValue(doc.getFieldValue("title"), url);
            String crawlDate = String.valueOf(doc.getFieldValue("crawlDate"));

            String snippet = "";
            if (highlighting != null && highlighting.containsKey(id)) {
                List<String> snippets = highlighting.get(id).get("content");
                if (snippets != null && !snippets.isEmpty()) {
                    snippet = String.join(" ... ", snippets);
                }
            }

            items.add(new SearchResultItem(id, url, title, snippet, crawlDate));
        }

        return new SearchResponseDto(
            rawQuery,
            response.getResults().getNumFound(),
            response.getQTime(),
            page,
            size,
            items
        );

    }

    private String firstOrValue(Object fieldVal, String fallback) {
        if (fieldVal instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.getFirst());
        }
        return fieldVal != null ? String.valueOf(fieldVal) : fallback;
    }
}
