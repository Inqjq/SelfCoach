package com.selfcoach.api;

import com.selfcoach.rag.search.SearchHit;
import com.selfcoach.rag.search.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 混合检索接口（第 6 步对外唯一入口）：
 * GET /api/search?q=<用户问题>&top=<条数，可选，默认5>
 * 返回 [{docId, chunkIndex, content, score}]，score 为 RRF 融合分，越大越相关。
 */
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 混合检索。
     * q 必填；top 可选，默认 5。返回结果已按融合分降序。
     */
    @GetMapping("/api/search")
    public Mono<List<SearchHit>> search(
            @RequestParam("q") String q,
            @RequestParam(name = "top", required = false, defaultValue = "5") int top) {
        return searchService.search(q, top);
    }
}