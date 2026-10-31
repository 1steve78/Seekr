#!/usr/bin/env bash
SOLR_URL="http://localhost:8983/solr/documents/schema"

# 1. Define the custom English text analyzer chain
curl -X POST -H 'Content-type:application/json' --data-binary '{
  "add-field-type": {
    "name": "text_en_search",
    "class": "solr.TextField",
    "positionIncrementGap": "100",
    "analyzer": {
      "tokenizer": { "class": "solr.StandardTokenizerFactory" },
      "filters": [
        { "class": "solr.LowerCaseFilterFactory" },
        { "class": "solr.StopFilterFactory", "ignoreCase": "true" },
        { "class": "solr.PorterStemFilterFactory" }
      ]
    }
  }
}' "$SOLR_URL"

# 2. Define the Phase 1 fields
curl -X POST -H 'Content-type:application/json' --data-binary '{
  "add-field": [
    { "name": "url",         "type": "string",         "stored": true, "indexed": true },
    { "name": "title",       "type": "text_en_search", "stored": true, "indexed": true },
    { "name": "content",     "type": "text_en_search", "stored": true, "indexed": true, "termVectors": true, "termPositions": true, "termOffsets": true },
    { "name": "lang",        "type": "string",         "stored": true, "indexed": true },
    { "name": "crawlDate",   "type": "pdate",          "stored": true, "indexed": true },
    { "name": "contentHash", "type": "string",         "stored": true, "indexed": true },
    { "name": "pagerank",    "type": "pfloat",         "stored": true, "indexed": true }
  ]
}' "$SOLR_URL"

# 3. Configure Soft Commit (1s for near-real-time visibility) and Hard Commit (30s for disk durability)
curl -X POST -H 'Content-type:application/json' --data-binary '{
  "set-property": {
    "updateHandler.autoSoftCommit.maxTime": 1000,
    "updateHandler.autoCommit.maxTime": 30000,
    "updateHandler.autoCommit.openSearcher": false
  }
}' "http://localhost:8983/solr/documents/config"