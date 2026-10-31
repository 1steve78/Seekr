package com.searchengine.nlp.extractor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.stream.Collectors;
@Component
public class BoilerplateExtractor {

    public ExtractedContent extract(String rawHtml,String url){
        Document doc = Jsoup.parse(rawHtml,url);
        String title = normalizeText(doc.title());

        doc.select("script, style, nav, footer, header, aside, noscript, iframe, svg, form, .ads, .sidebar, #cookie-banner")
           .remove();

        Element mainContainer = doc.selectFirst("article, main, [role=main]");
        if (mainContainer == null || mainContainer.text().length() < 200) {
            mainContainer = doc.body();
        }

        if (mainContainer == null) {
            return new ExtractedContent(title, "");
        }

        String cleanBody = mainContainer.select("h1, h2, h3, p, li, pre")
                .stream()
                .map(Element::text)
                .filter(text -> text.length() > 25)
                .collect(Collectors.joining("\n"));

        if(cleanBody.length() < 150){
            cleanBody = mainContainer.text();
        }

        return new ExtractedContent(title, normalizeText(cleanBody));
    }

    private String normalizeText(String input){
        if(input == null) return  "";

        return Normalizer.normalize(input, Normalizer.Form.NFC)
            .replaceAll("\\s+", " ")
            .trim();
    }
    public record ExtractedContent(String title, String body) {}
}
