package com.searchengine.crawler.util;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Collectors;



public class UrlNormalizer{
    public static String normalize(String rawUrl){
        try{
            URI uri = new URI(rawUrl.trim());
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "https";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            if(host.startsWith("www.")) host = host.substring(4);

            String path = (uri.getPath() == null || uri.getPath().isEmpty()) ? "/" : uri.getPath();
            if(path.length() > 1 && path.endsWith("/")){
                path = path.substring(0,path.length() -1);
            }

            String query  = uri.getQuery();
            if(query != null && !query.isEmpty()){
                query = Arrays.stream(query.split("&"))
                                .filter(p -> {
                                    String parameter = p.toLowerCase(Locale.ROOT);
                                    return !parameter.startsWith("utm") && !parameter.startsWith("fbclid=");
                                })
                                .sorted()
                                .collect(Collectors.joining("&"));
            
            }
            
            return new URI(scheme,null,host,uri.getPort(),path,
                            (query == null || query.isEmpty() ) ? null : query , null).toString();
                           
        } catch (Exception e){
            return null;
        }
    }
    
    public static String sha256(String input) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}