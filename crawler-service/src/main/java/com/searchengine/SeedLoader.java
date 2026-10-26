package com.searchengine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.searchengine.crawler.frontier.RedisUrlFrontier;

@Component 
public class SeedLoader  implements  CommandLineRunner{
    
    private final RedisUrlFrontier frontier;

    public SeedLoader(RedisUrlFrontier frontier){
        this.frontier = frontier;
    }

    @Override
    public void run(String... args) throws Exception {
        var resource = new ClassPathResource("seed.txt");
        try (var reader = new BufferedReader(
                new InputStreamReader(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8))) 
        {
            reader.lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> !line.startsWith("#"))
            .forEach(url -> frontier.pushUrl(url, 0.0));
        }
    }
    
}
