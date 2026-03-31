package com.example.javarag.controller;

import com.example.javarag.dto.AskRequest;
import com.example.javarag.dto.AskResponse;
import com.example.javarag.dto.IndexRequest;
import com.example.javarag.service.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/index")
    public Map<String, Object> index(@Valid @RequestBody IndexRequest request) {
        ragService.indexDocument(request.id(), request.content(), request.metadata(), true);
        return Map.of(
                "message", "indexed",
                "id", request.id(),
                "totalChunks", ragService.totalChunks()
        );
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return ragService.ask(request.question(), request.topK());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "chunks", ragService.totalChunks()
        );
    }
}
