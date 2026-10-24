package com.searchengine.crawler.frontier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class RedisUrlFrontierTest {

    private StringRedisTemplate redis;
    private SetOperations<String, String> sets;
    private ZSetOperations<String, String> sortedSets;
    private RedisUrlFrontier frontier;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        sets = mock(SetOperations.class);
        sortedSets = mock(ZSetOperations.class);
        when(redis.opsForSet()).thenReturn(sets);
        when(redis.opsForZSet()).thenReturn(sortedSets);
        frontier = new RedisUrlFrontier(redis);
    }

    @Test
    void addsNewNormalizedUrlToItsHostQueue() {
        when(sets.add(anyString(), anyString())).thenReturn(1L);
        when(sortedSets.addIfAbsent(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(true);

        assertTrue(frontier.pushUrl("https://www.example.com/page/", 0.5));

        verify(sortedSets).add("crawler:queue:example.com", "https://example.com/page", 0.5);
        verify(sortedSets).addIfAbsent(
                eq("crawler:host_schedule"), eq("example.com"), org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void doesNotQueueDuplicateUrls() {
        when(sets.add(anyString(), anyString())).thenReturn(0L);

        assertFalse(frontier.pushUrl("https://example.com/page", 0.5));

        verify(sortedSets, never()).add(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void rejectsMalformedUrlsBeforeAccessingRedis() {
        assertFalse(frontier.pushUrl("not a url", 0.5));

        verify(redis, never()).opsForSet();
    }
}
