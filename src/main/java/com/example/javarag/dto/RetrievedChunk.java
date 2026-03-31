package com.example.javarag.dto;

import java.util.Map;

public record RetrievedChunk(
        String chunkId,
        String documentId,
        double score,
        String content,
        Map<String, Object> metadata
) {
}
