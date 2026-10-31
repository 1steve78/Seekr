package com.searchengine.controller;

import com.searchengine.dto.SearchResponseDto;
import com.searchengine.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller 
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService){
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    @ResponseBody
    public SearchResponseDto searchApi(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) throws Exception {
        return searchService.search(query, page, Math.min(size, 50));
    }

    @GetMapping("/")
    public String searchPage(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) throws Exception {
        SearchResponseDto result = searchService.search(query, page, 10);
        model.addAttribute("res", result);
        return "index";
    }
}
