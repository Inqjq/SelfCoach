package com.selfcoach.api;

import com.selfcoach.rag.chat.ChatResponse;
import com.selfcoach.rag.chat.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 问答接口（第 7 步 / F-03 收尾）：
 * POST /api/chat  body: {"question":"深蹲用什么肌群"}
 * 返回 {"answer":"...", "sources":[{docId, chunkIndex, content}]}。
 */
@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 提问。入参用 Map 接收（与 IngestController 风格一致），只取 question 字段。
     * 所有降级都在 Service 层处理，这里正常 200 返回。
     */
    @PostMapping("/api/chat")
    public Mono<ChatResponse> chat(@RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "");
        return chatService.answer(question);
    }
}