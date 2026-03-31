package com.example.javarag.dto;

import java.util.List;

public record AskResponse(
        String answer,
        List<RetrievedChunk> references,
        boolean usedRealOpenAi
) {
}
