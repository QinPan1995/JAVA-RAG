package com.example.javarag.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record IndexRequest(
        @NotBlank(message = "id 不能为空")
        String id,
        @NotBlank(message = "content 不能为空")
        String content,
        Map<String, Object> metadata
) {
}
