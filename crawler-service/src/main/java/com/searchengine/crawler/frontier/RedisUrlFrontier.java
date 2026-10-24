package com.searchengine.crawler.frontier;

import java.net.URI;
import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.searchengine.crawler.util.UrlNormalizer;

@Service 
public  class RedisUrlFrontier {

    private final StringRedisTemplate redis;
    private static final String SEEN_SET = "crawler:seen_urls";
    private static final String HOST_SCHEDULE = "crawler:host_schedule";
    private static final long POLITENESS_DELAY_MS = 2000;

    private static final String POP_SCRIPT = """
        local now = tonumber(ARGV[1])
        local delay = tonumber(ARGV[2])
        local hosts = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', now, 'LIMIT', 0, 1)
        if #hosts == 0 then return nil end
        local host = hosts[1]
        local queueKey = 'crawler:queue:' .. host
        local item = redis.call('ZPOPMIN', queueKey, 1)
        if #item == 0 then
            redis.call('ZREM', KEYS[1], host)
            return nil
        end
        if redis.call('ZCARD', queueKey) == 0 then
            redis.call('ZREM', KEYS[1], host)
        else
            redis.call('ZADD', KEYS[1], now + delay, host)
        end
        return item[1]
        """;

    public  RedisUrlFrontier(StringRedisTemplate redis){
        this.redis = redis;
    }

    public boolean pushUrl(String rawUrl,double priorityScore){
        String normalized =  UrlNormalizer.normalize(rawUrl);
        if(normalized == null) return false;

        String urlHash = UrlNormalizer.sha256(normalized);
        Boolean isNew = redis.opsForSet().add(SEEN_SET, urlHash) == 1;
        if(Boolean.FALSE.equals(isNew)) return  false;

        String host = URI.create(normalized).getHost();
        if(host == null) return false;

        redis.opsForZSet().add("crawler:queue:" + host ,normalized,priorityScore);

        redis.opsForZSet().addIfAbsent(HOST_SCHEDULE,host,System.currentTimeMillis());

        return true;
    }

    public String popNextReadyUrl(){
        DefaultRedisScript<String> script = new DefaultRedisScript<>(POP_SCRIPT,String.class);
        return redis.execute(script,Collections.singletonList(HOST_SCHEDULE) , String.valueOf(System.currentTimeMillis()),String.valueOf(POLITENESS_DELAY_MS));
    }

}