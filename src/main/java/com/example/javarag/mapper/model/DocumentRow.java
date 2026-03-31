package com.example.javarag.mapper.model;

public record DocumentRow(
        String documentId,
        String content,
        String metadata
) {
}
