package com.searchengine.crawler.politeness;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RobotsTxtService {

    private static final String USER_AGENT = "SearchEngineBot/1.0";
    private final OkHttpClient httpClient;
    private final SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
    private final Map<String, BaseRobotRules> rulesCache = new ConcurrentHashMap<>();

    public RobotsTxtService(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public boolean isAllowed(String url) {
        URI uri = URI.create(url);
        String hostRoot = uri.getScheme() + "://" + uri.getHost()
            + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        BaseRobotRules rules = rulesCache.computeIfAbsent(hostRoot, this::fetchRobotsTxt);
        return rules.isAllowed(url);
    }

    private BaseRobotRules fetchRobotsTxt(String hostRoot) {
        Request request = new Request.Builder()
            .url(hostRoot + "/robots.txt")
            .header("User-Agent", USER_AGENT)
            .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                byte[] bytes = response.body().bytes();
                return parser.parseContent(hostRoot + "/robots.txt", bytes, "text/plain", USER_AGENT);
            }
        } catch (Exception ignored) {}
    
        return new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
    }
}