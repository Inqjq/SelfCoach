package com.selfcoach.api;

import com.selfcoach.rag.ingestion.IngestService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 入库接口（第 5 步验证用 + 后续接知识库管理）：
 * POST /api/ingest  body: {"docId":"d1","source":"健身指南.md","content":"<整篇文档文本>"}
 * 会把文档分块、向量化后写入 doc_chunk，返回写入的分块数。
 */
@RestController
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/api/ingest")
    public Mono<Map<String, Object>> ingest(@RequestBody Map<String, String> body) {
        String docId = body.getOrDefault("docId", "doc-" + System.currentTimeMillis());
        String source = body.getOrDefault("source", "");
        String content = body.getOrDefault("content", "");
        return ingestService.ingest(docId, source, content)
                .map(n -> Map.of(
                        "docId", docId,
                        "ingestedChunks", n,
                        "message", n + " 个分块已写入 doc_chunk"
                ));
    }
}