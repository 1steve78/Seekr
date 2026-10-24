package com.searchengine.crawler.worker;

import com.searchengine.common.model.CrawledPage;
import com.searchengine.crawler.frontier.RedisUrlFrontier;
import com.searchengine.crawler.politeness.RobotsTxtService;
import com.searchengine.crawler.util.UrlNormalizer;
import jakarta.annotation.PostConstruct;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Executors;

@Component 
public class CrawlerWorker {
    
    private static final long MAX_BYTES = 2 *1024 * 1024;
    private static final String SEEN_CONTENT_HASHES = "crawler:seen_content";

    private final RedisUrlFrontier frontier;
    private final RobotsTxtService robotsService;
    private final OkHttpClient httpClient ;
    private final StringRedisTemplate  redis;
    private final KafkaTemplate<String,CrawledPage> kafkaTemplate;

    public CrawlerWorker(RedisUrlFrontier frontier , RobotsTxtService robotsService,
                    OkHttpClient httpClient,StringRedisTemplate redis,
                    KafkaTemplate<String,CrawledPage> kafkaTemplate
    ){
        this.frontier = frontier;
        this.robotsService = robotsService;
        this.httpClient = httpClient;
        this.redis = redis;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct 
    public void startWorkers(){
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        for(int i = 0 ; i < 32 ; i++){
            executor.submit(this::crawlLoop);
        }
    }

    private void crawlLoop(){
        while (!Thread.currentThread().isInterrupted()) {
            try{
                String url = frontier.popNextReadyUrl();
                if(url == null){
                    Thread.sleep(200);
                    continue;
                }
                if(!robotsService.isAllowed(url)) continue;

                processUrl(url);
            }catch (Exception e){}
        }
    }
    
    private void processUrl(String url) throws Exception{
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "SearchEngineBot/1.0 (+http://localhost)")
                .build();

        try (Response res = httpClient.newCall(req).execute()){
            if(!res.isSuccessful() || res.body() == null)  return ;

            String contentType = res.header("Content-Type", "");
            if(!contentType.contains("text/html")) return ;

            byte[] rawBytes = res.body().source().inputStream().readNBytes((int) MAX_BYTES);
            String html = new String(rawBytes);

            Document doc = Jsoup.parse(html,url);
            String plainText = doc.body() != null ? doc.body().text() :"";
            if(plainText.length() < 100) return ;

            String contentHash = UrlNormalizer.sha256(plainText);
            Boolean isUniqueContent = redis.opsForSet().add(SEEN_CONTENT_HASHES,contentHash) == 1;
            if (Boolean.FALSE.equals(isUniqueContent)) return;

            String fileId = UrlNormalizer.sha256(url) + ".html";
            Path outPath = Path.of("/tmp/crawl-data" ,fileId);
            Files.createDirectories(outPath.getParent());
            Files.writeString(outPath,html);

            for(Element link : doc.select("a[href]")){
                String absUrl = link.attr("abs:href");
                if(absUrl.startsWith("http")){
                    frontier.pushUrl(absUrl, 1.0);
                }
            }

            CrawledPage event = new CrawledPage(url,Instant.now(),res.code(),outPath.toString() , contentHash );
            kafkaTemplate.send("raw-pages",url,event);
        }
    }
}
